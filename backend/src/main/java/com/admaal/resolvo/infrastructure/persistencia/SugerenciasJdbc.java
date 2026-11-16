package com.admaal.resolvo.infrastructure.persistencia;

import com.admaal.resolvo.application.Sugerencias;
import com.admaal.resolvo.domain.Cita;
import com.admaal.resolvo.domain.EstadoSugerencia;
import com.admaal.resolvo.domain.FragmentoId;
import com.admaal.resolvo.domain.FuenteConocimiento;
import com.admaal.resolvo.domain.OrganizacionId;
import com.admaal.resolvo.domain.Sugerencia;
import com.admaal.resolvo.domain.SugerenciaId;
import com.admaal.resolvo.domain.TicketId;
import com.admaal.resolvo.domain.TransicionNoPermitida;
import com.admaal.resolvo.domain.UsuarioId;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public class SugerenciasJdbc implements Sugerencias {

  private static final String COLUMNAS =
      "id, organizacion_id, ticket_id, borrador, estado, modelo, creada_en, resuelta_por, resuelta_en";

  private final JdbcTemplate jdbc;

  public SugerenciasJdbc(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  /** Sugerencia y citas se escriben juntas: si falla una cita no queda una pendiente a medias. */
  @Override
  @Transactional
  public void guardar(Sugerencia sugerencia) {
    Integer existe =
        jdbc.queryForObject(
            "select count(*) from sugerencia where id = ?", Integer.class, sugerencia.id().valor());
    if (existe == null || existe == 0) {
      insertar(sugerencia);
      return;
    }
    if (sugerencia.estado() == EstadoSugerencia.PENDIENTE) {
      return;
    }
    // Solo una pendiente puede resolverse: si dos personas resuelven a la vez, la segunda pierde.
    int cambiadas =
        jdbc.update(
            """
            update sugerencia set estado = ?, resuelta_por = ?, resuelta_en = ?
            where id = ? and estado = 'PENDIENTE'
            """,
            sugerencia.estado().name(),
            sugerencia.resueltaPor().valor(),
            Timestamp.from(sugerencia.resueltaEn()),
            sugerencia.id().valor());
    if (cambiadas == 0) {
      throw new TransicionNoPermitida();
    }
  }

  private void insertar(Sugerencia sugerencia) {
    jdbc.update(
        """
        insert into sugerencia (id, organizacion_id, ticket_id, borrador, estado, modelo, creada_en)
        values (?, ?, ?, ?, ?, ?, ?)
        """,
        sugerencia.id().valor(),
        sugerencia.organizacionId().valor(),
        sugerencia.ticketId().valor(),
        sugerencia.borrador(),
        sugerencia.estado().name(),
        sugerencia.modelo(),
        Timestamp.from(sugerencia.creadaEn()));
    for (Cita cita : sugerencia.citas()) {
      // Si reindexar ya reemplazo el fragmento, el select no devuelve fila y la cita se omite.
      jdbc.update(
          """
          insert into cita (sugerencia_id, orden, fragmento_id, fuente, fuente_id, titulo)
          select ?, ?, id, fuente, fuente_id, titulo from fragmento where id = ?
          """,
          sugerencia.id().valor(),
          cita.orden(),
          cita.fragmentoId().valor());
    }
  }

  @Override
  public Optional<Sugerencia> porId(OrganizacionId organizacionId, SugerenciaId id) {
    return jdbc
        .query(
            "select " + COLUMNAS + " from sugerencia where id = ? and organizacion_id = ?",
            this::fila,
            id.valor(),
            organizacionId.valor())
        .stream()
        .findFirst();
  }

  @Override
  public Optional<Sugerencia> pendienteDe(OrganizacionId organizacionId, TicketId ticketId) {
    return jdbc
        .query(
            "select "
                + COLUMNAS
                + " from sugerencia where organizacion_id = ? and ticket_id = ? and estado = 'PENDIENTE'",
            this::fila,
            organizacionId.valor(),
            ticketId.valor())
        .stream()
        .findFirst();
  }

  public List<CitaDetalle> citasDe(OrganizacionId organizacionId, SugerenciaId id) {
    return jdbc.query(
        """
        select c.orden, c.fuente, c.fuente_id, c.titulo
        from cita c join sugerencia s on s.id = c.sugerencia_id
        where c.sugerencia_id = ? and s.organizacion_id = ?
        order by c.orden
        """,
        (fila, numero) ->
            new CitaDetalle(
                fila.getInt("orden"),
                FuenteConocimiento.valueOf(fila.getString("fuente")),
                fila.getObject("fuente_id", UUID.class),
                fila.getString("titulo")),
        id.valor(),
        organizacionId.valor());
  }

  private Sugerencia fila(ResultSet fila, int numero) throws SQLException {
    SugerenciaId id = new SugerenciaId(fila.getObject("id", UUID.class));
    List<Cita> citas =
        jdbc.query(
            "select fragmento_id, orden from cita where sugerencia_id = ? order by orden",
            (c, n) -> new Cita(new FragmentoId(c.getObject("fragmento_id", UUID.class)), c.getInt("orden")),
            id.valor());
    UUID resueltaPor = fila.getObject("resuelta_por", UUID.class);
    Timestamp resueltaEn = fila.getTimestamp("resuelta_en");
    return Sugerencia.rehidratar(
        id,
        new OrganizacionId(fila.getObject("organizacion_id", UUID.class)),
        new TicketId(fila.getObject("ticket_id", UUID.class)),
        fila.getString("borrador"),
        fila.getString("modelo"),
        fila.getTimestamp("creada_en").toInstant(),
        citas,
        EstadoSugerencia.valueOf(fila.getString("estado")),
        resueltaPor == null ? null : new UsuarioId(resueltaPor),
        resueltaEn == null ? null : resueltaEn.toInstant());
  }
}
