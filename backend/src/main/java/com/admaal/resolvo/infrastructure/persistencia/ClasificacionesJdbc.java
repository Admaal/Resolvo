package com.admaal.resolvo.infrastructure.persistencia;

import com.admaal.resolvo.application.Clasificaciones;
import com.admaal.resolvo.domain.CategoriaId;
import com.admaal.resolvo.domain.Clasificacion;
import com.admaal.resolvo.domain.ClasificacionId;
import com.admaal.resolvo.domain.EstadoClasificacion;
import com.admaal.resolvo.domain.OrganizacionId;
import com.admaal.resolvo.domain.Prioridad;
import com.admaal.resolvo.domain.TicketId;
import com.admaal.resolvo.domain.TransicionNoPermitida;
import com.admaal.resolvo.domain.UsuarioId;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class ClasificacionesJdbc implements Clasificaciones {

  private static final String COLUMNAS =
      "id, organizacion_id, ticket_id, categoria_id, prioridad, motivo, estado, modelo, creada_en,"
          + " resuelta_por, resuelta_en";

  private final JdbcTemplate jdbc;

  public ClasificacionesJdbc(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  @Override
  public void guardar(Clasificacion clasificacion) {
    Integer existe =
        jdbc.queryForObject(
            "select count(*) from clasificacion where id = ?",
            Integer.class,
            clasificacion.id().valor());
    if (existe == null || existe == 0) {
      insertar(clasificacion);
      return;
    }
    if (clasificacion.estado() == EstadoClasificacion.PENDIENTE) {
      return;
    }
    // Solo una pendiente puede resolverse: si dos personas resuelven a la vez, la segunda pierde.
    int cambiadas =
        jdbc.update(
            """
            update clasificacion set estado = ?, resuelta_por = ?, resuelta_en = ?
            where id = ? and estado = 'PENDIENTE'
            """,
            clasificacion.estado().name(),
            clasificacion.resueltaPor().valor(),
            Timestamp.from(clasificacion.resueltaEn()),
            clasificacion.id().valor());
    if (cambiadas == 0) {
      throw new TransicionNoPermitida();
    }
  }

  private void insertar(Clasificacion clasificacion) {
    jdbc.update(
        """
        insert into clasificacion
          (id, organizacion_id, ticket_id, categoria_id, prioridad, motivo, estado, modelo, creada_en)
        values (?, ?, ?, ?, ?, ?, ?, ?, ?)
        """,
        clasificacion.id().valor(),
        clasificacion.organizacionId().valor(),
        clasificacion.ticketId().valor(),
        clasificacion.categoriaId().valor(),
        clasificacion.prioridad().name(),
        clasificacion.motivo(),
        clasificacion.estado().name(),
        clasificacion.modelo(),
        Timestamp.from(clasificacion.creadaEn()));
  }

  @Override
  public Optional<Clasificacion> porId(OrganizacionId organizacionId, ClasificacionId id) {
    return jdbc
        .query(
            "select " + COLUMNAS + " from clasificacion where id = ? and organizacion_id = ?",
            this::fila,
            id.valor(),
            organizacionId.valor())
        .stream()
        .findFirst();
  }

  @Override
  public Optional<Clasificacion> pendienteDe(OrganizacionId organizacionId, TicketId ticketId) {
    return jdbc
        .query(
            "select "
                + COLUMNAS
                + " from clasificacion where organizacion_id = ? and ticket_id = ?"
                + " and estado = 'PENDIENTE'",
            this::fila,
            organizacionId.valor(),
            ticketId.valor())
        .stream()
        .findFirst();
  }

  private Clasificacion fila(ResultSet fila, int numero) throws SQLException {
    UUID resueltaPor = fila.getObject("resuelta_por", UUID.class);
    Timestamp resueltaEn = fila.getTimestamp("resuelta_en");
    return Clasificacion.rehidratar(
        new ClasificacionId(fila.getObject("id", UUID.class)),
        new OrganizacionId(fila.getObject("organizacion_id", UUID.class)),
        new TicketId(fila.getObject("ticket_id", UUID.class)),
        new CategoriaId(fila.getObject("categoria_id", UUID.class)),
        Prioridad.valueOf(fila.getString("prioridad")),
        fila.getString("motivo"),
        fila.getString("modelo"),
        fila.getTimestamp("creada_en").toInstant(),
        EstadoClasificacion.valueOf(fila.getString("estado")),
        resueltaPor == null ? null : new UsuarioId(resueltaPor),
        resueltaEn == null ? null : resueltaEn.toInstant());
  }
}
