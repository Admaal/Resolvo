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
      FuenteConocimiento fuente, UUID fuenteId, String hashFuente, String modelo) {
    Integer filas =
        jdbc.queryForObject(
            """
            select count(*) from (
              select 1 from fragmento
              where fuente = ? and fuente_id = ? and hash_fuente = ? and modelo = ?
              limit 1) existente
            """,
            Integer.class,
            fuente.name(),
            fuenteId,
            hashFuente,
            modelo);
    return filas != null && filas > 0;
  }

  @Override
  public void reemplazar(
      FuenteConocimiento fuente,
      UUID fuenteId,
      String hashFuente,
      String modelo,
      List<FragmentoIndexado> fragmentos) {
    jdbc.update("delete from fragmento where fuente = ? and fuente_id = ?", fuente.name(), fuenteId);
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
  public List<Fragmento> buscar(
      OrganizacionId organizacionId, Embedding consulta, int k, UUID excluirFuenteId) {
    return jdbc.query(
        """
        select id, organizacion_id, fuente, fuente_id, titulo, texto
        from fragmento
        where organizacion_id = ? and fuente_id <> ?
        order by embedding <=> ?::vector
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
        literal(consulta),
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
