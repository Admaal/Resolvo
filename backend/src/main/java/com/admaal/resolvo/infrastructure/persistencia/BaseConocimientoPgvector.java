package com.admaal.resolvo.infrastructure.persistencia;

import com.admaal.resolvo.application.BaseConocimiento;
import com.admaal.resolvo.application.FragmentoIndexado;
import com.admaal.resolvo.domain.Embedding;
import com.admaal.resolvo.domain.Fragmento;
import com.admaal.resolvo.domain.FragmentoId;
import com.admaal.resolvo.domain.FuenteConocimiento;
import com.admaal.resolvo.domain.OrganizacionId;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

/**
 * Busqueda vectorial con JdbcTemplate y no con PgVectorStore: el filtro por organizacion es una
 * columna con indice, no un metadato JSON, y no puede olvidarse en una consulta.
 */
@Repository
public class BaseConocimientoPgvector implements BaseConocimiento {

  private final JdbcTemplate jdbc;

  public BaseConocimientoPgvector(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  @Override
  public boolean estaIndexada(
      OrganizacionId organizacionId,
      FuenteConocimiento fuente,
      UUID fuenteId,
      String hashFuente,
      String modelo) {
    Integer filas =
        jdbc.queryForObject(
            """
            select count(*) from (
              select 1 from fragmento
              where organizacion_id = ? and fuente = ? and fuente_id = ?
                and hash_fuente = ? and modelo = ?
              limit 1) existente
            """,
            Integer.class,
            organizacionId.valor(),
            fuente.name(),
            fuenteId,
            hashFuente,
            modelo);
    return filas != null && filas > 0;
  }

  /**
   * Borra e inserta en una sola transaccion: una caida a mitad no deja la fuente incompleta. El
   * cerrojo por fuente serializa dos indexados simultaneos (arranque y reindexado manual).
   */
  @Override
  @Transactional
  public void reemplazar(
      OrganizacionId organizacionId,
      FuenteConocimiento fuente,
      UUID fuenteId,
      String hashFuente,
      String modelo,
      List<FragmentoIndexado> fragmentos) {
    jdbc.queryForList(
        "select pg_advisory_xact_lock(hashtextextended(?, 0))", fuente.name() + ":" + fuenteId);
    jdbc.update(
        "delete from fragmento where organizacion_id = ? and fuente = ? and fuente_id = ?",
        organizacionId.valor(),
        fuente.name(),
        fuenteId);
    int orden = 0;
    for (FragmentoIndexado indexado : fragmentos) {
      Fragmento fragmento = indexado.fragmento();
      jdbc.update(
          """
          insert into fragmento
            (id, organizacion_id, fuente, fuente_id, hash_fuente, modelo, orden, titulo, texto, embedding)
          values (?, ?, ?, ?, ?, ?, ?, ?, ?, ?::vector)
          """,
          fragmento.id().valor(),
          fragmento.organizacionId().valor(),
          fuente.name(),
          fuenteId,
          hashFuente,
          modelo,
          orden++,
          fragmento.titulo(),
          fragmento.texto(),
          literal(indexado.embedding()));
    }
  }

  @Override
  public void borrar(OrganizacionId organizacionId, FuenteConocimiento fuente, UUID fuenteId) {
    jdbc.update(
        "delete from fragmento where organizacion_id = ? and fuente = ? and fuente_id = ?",
        organizacionId.valor(),
        fuente.name(),
        fuenteId);
  }

  /**
   * Busqueda exacta dentro de la organizacion. Ordenar por la expresion pelada usaria el indice
   * HNSW, que filtra por organizacion despues de elegir los vecinos: con otra organizacion mucho
   * mayor devolveria pocas filas o ninguna. El "+ 0" impide usar el indice y obliga a filtrar
   * primero; con cientos o pocos miles de fragmentos por organizacion es barato.
   */
  @Override
  public List<Fragmento> buscar(
      OrganizacionId organizacionId,
      Embedding consulta,
      int k,
      UUID excluirFuenteId,
      double distanciaMaxima) {
    String vector = literal(consulta);
    return jdbc.query(
        """
        select id, organizacion_id, fuente, fuente_id, titulo, texto
        from fragmento
        where organizacion_id = ? and fuente_id <> ?
          and (embedding <=> ?::vector) + 0 <= ?
        order by (embedding <=> ?::vector) + 0
        limit ?
        """,
        (fila, numero) ->
            new Fragmento(
                new FragmentoId(fila.getObject("id", UUID.class)),
                new OrganizacionId(fila.getObject("organizacion_id", UUID.class)),
                FuenteConocimiento.valueOf(fila.getString("fuente")),
                fila.getObject("fuente_id", UUID.class),
                fila.getString("titulo"),
                fila.getString("texto")),
        organizacionId.valor(),
        excluirFuenteId,
        vector,
        distanciaMaxima,
        vector,
        k);
  }

  private static String literal(Embedding embedding) {
    StringBuilder texto = new StringBuilder("[");
    float[] valores = embedding.valores();
    for (int i = 0; i < valores.length; i++) {
      if (i > 0) {
        texto.append(',');
      }
      texto.append(valores[i]);
    }
    return texto.append(']').toString();
  }
}
