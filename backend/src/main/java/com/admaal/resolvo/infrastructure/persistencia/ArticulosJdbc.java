package com.admaal.resolvo.infrastructure.persistencia;

import com.admaal.resolvo.application.Articulos;
import com.admaal.resolvo.domain.Articulo;
import com.admaal.resolvo.domain.OrganizacionId;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class ArticulosJdbc implements Articulos {

  private final JdbcTemplate jdbc;

  public ArticulosJdbc(JdbcTemplate jdbc) {
    this.jdbc = jdbc;
  }

  @Override
  public List<Articulo> deLaOrganizacion(OrganizacionId organizacionId) {
    return jdbc.query(
        "select id, organizacion_id, titulo, cuerpo from articulo where organizacion_id = ? order by titulo",
        this::fila,
        organizacionId.valor());
  }

  @Override
  public Optional<Articulo> porId(OrganizacionId organizacionId, UUID id) {
    return jdbc
        .query(
            "select id, organizacion_id, titulo, cuerpo from articulo where id = ? and organizacion_id = ?",
            this::fila,
            id,
            organizacionId.valor())
        .stream()
        .findFirst();
  }

  private Articulo fila(ResultSet fila, int numero) throws SQLException {
    return new Articulo(
        fila.getObject("id", UUID.class),
        new OrganizacionId(fila.getObject("organizacion_id", UUID.class)),
        fila.getString("titulo"),
        fila.getString("cuerpo"));
  }
}
