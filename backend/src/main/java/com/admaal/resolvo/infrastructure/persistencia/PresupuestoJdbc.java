package com.admaal.resolvo.infrastructure.persistencia;

import com.admaal.resolvo.application.Presupuesto;
import com.admaal.resolvo.application.PresupuestoAgotado;
import com.admaal.resolvo.application.RegistroLlamadas;
import com.admaal.resolvo.application.Reloj;
import com.admaal.resolvo.domain.Consumo;
import com.admaal.resolvo.domain.OrganizacionId;
import com.admaal.resolvo.domain.PresupuestoDiario;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Presupuesto diario por organizacion (dia UTC). Suma lo ya registrado en {@code llamada_ia} y las
 * reservas abiertas de {@code presupuesto_reserva}, dentro de una transaccion con un cerrojo
 * consultivo por organizacion: dos peticiones simultaneas no pasan juntas por la ultima llamada
 * libre aunque las atiendan instancias distintas, y una organizacion no espera a otra.
 */
@Component
public class PresupuestoJdbc implements Presupuesto {

  /** Mas que lo que dura una sugerencia con todos sus reintentos: una reserva huerfana se libera sola. */
  static final Duration CADUCIDAD_DE_RESERVA = Duration.ofMinutes(5);

  private final JdbcTemplate jdbc;
  private final TransactionTemplate transacciones;
  private final RegistroLlamadas registro;
  private final Reloj reloj;
  private final PresupuestoDiario limite;

  @Autowired
  public PresupuestoJdbc(
      JdbcTemplate jdbc,
      TransactionTemplate transacciones,
      RegistroLlamadas registro,
      Reloj reloj,
      @Value("${resolvo.ia.presupuesto-diario.llamadas:200}") int llamadas,
      @Value("${resolvo.ia.presupuesto-diario.tokens:300000}") long tokens) {
    this(jdbc, transacciones, registro, reloj, new PresupuestoDiario(llamadas, tokens));
  }

  public PresupuestoJdbc(
      JdbcTemplate jdbc,
      TransactionTemplate transacciones,
      RegistroLlamadas registro,
      Reloj reloj,
      PresupuestoDiario limite) {
    this.jdbc = jdbc;
    this.transacciones = transacciones;
    this.registro = registro;
    this.reloj = reloj;
    this.limite = limite;
  }

  @Override
  public Reserva reservar(OrganizacionId organizacionId) {
    Instant ahora = reloj.ahora();
    UUID id = UUID.randomUUID();
    transacciones.executeWithoutResult(
        estado -> {
          jdbc.queryForObject(
              "select pg_advisory_xact_lock(hashtextextended(?, 0))",
              Object.class,
              "presupuesto|" + organizacionId.valor());
          jdbc.update(
              "delete from presupuesto_reserva where organizacion_id = ? and caduca_en <= ?",
              organizacionId.valor(),
              Timestamp.from(ahora));
          Integer abiertas =
              jdbc.queryForObject(
                  "select count(*) from presupuesto_reserva where organizacion_id = ?",
                  Integer.class,
                  organizacionId.valor());
          Instant desde = ahora.truncatedTo(ChronoUnit.DAYS);
          Instant hasta =
              LocalDate.ofInstant(desde, ZoneOffset.UTC)
                  .plusDays(1)
                  .atStartOfDay(ZoneOffset.UTC)
                  .toInstant();
          Consumo consumo = registro.consumoDe(organizacionId, desde, hasta);
          if (!limite.admite(consumo, abiertas == null ? 0 : abiertas)) {
            throw new PresupuestoAgotado();
          }
          jdbc.update(
              "insert into presupuesto_reserva (id, organizacion_id, caduca_en) values (?, ?, ?)",
              id,
              organizacionId.valor(),
              Timestamp.from(ahora.plus(CADUCIDAD_DE_RESERVA)));
        });
    AtomicBoolean cerrada = new AtomicBoolean();
    return () -> {
      if (cerrada.compareAndSet(false, true)) {
        jdbc.update("delete from presupuesto_reserva where id = ?", id);
      }
    };
  }
}
