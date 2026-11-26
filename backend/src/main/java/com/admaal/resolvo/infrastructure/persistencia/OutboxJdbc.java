package com.admaal.resolvo.infrastructure.persistencia;

import com.admaal.resolvo.application.BandejaEventos;
import com.admaal.resolvo.application.EventosProcesados;
import com.admaal.resolvo.application.PublicadorEventos;
import com.admaal.resolvo.domain.EventoTicket;
import com.admaal.resolvo.domain.OrganizacionId;
import com.admaal.resolvo.domain.TicketId;
import com.admaal.resolvo.domain.TipoEvento;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import org.springframework.context.ApplicationEventPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Bandeja de salida en Postgres. Publicar inserta en la transaccion en curso; reclamar toma los
 * pendientes con {@code FOR UPDATE SKIP LOCKED} y un reclamo con vencimiento, de modo que dos
 * procesadores no se pisan y un evento cuyo procesador cae vuelve a estar disponible.
 */
@Repository
public class OutboxJdbc implements PublicadorEventos, BandejaEventos, EventosProcesados {

  private static final Logger LOG = LoggerFactory.getLogger(OutboxJdbc.class);

  /** Aviso interno de que hay algo nuevo; se entrega al confirmarse la transaccion. */
  public record EventoPublicado(UUID id) {}

  /** Segundos que un procesador conserva un evento reclamado antes de que otro pueda tomarlo. */
  static final int SEGUNDOS_DE_RECLAMO = 120;

  /** Espera antes de reintentar un evento cuyo consumidor fallo; menos que el barrido de un minuto. */
  static final int SEGUNDOS_DE_REINTENTO = 30;

  private final JdbcTemplate jdbc;
  private final TransactionTemplate propia;
  private final ApplicationEventPublisher avisos;

  public OutboxJdbc(
      JdbcTemplate jdbc, PlatformTransactionManager transacciones, ApplicationEventPublisher avisos) {
    this.jdbc = jdbc;
    this.propia = new TransactionTemplate(transacciones);
    this.propia.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    this.avisos = avisos;
  }

  @Override
  public void publicar(EventoTicket evento) {
    jdbc.update(
        """
        insert into evento_outbox (id, tipo, organizacion_id, ticket_id, ocurrido_en)
        values (?, ?, ?, ?, ?)
        """,
        evento.id(),
        evento.tipo().name(),
        evento.organizacionId().valor(),
        evento.ticketId().valor(),
        Timestamp.from(evento.ocurridoEn()));
    avisos.publishEvent(new EventoPublicado(evento.id()));
  }

  @Override
  public List<Reclamado> reclamar(int maximo) {
    List<Reclamado> reclamados =
        propia.execute(
            estado ->
                jdbc.query(
                    """
                    update evento_outbox
                    set reclamado_hasta = now() + make_interval(secs => ?), intentos = intentos + 1
                    where id in (
                      select id from evento_outbox
                      where estado = 'PENDIENTE' and (reclamado_hasta is null or reclamado_hasta < now())
                      order by ocurrido_en, id
                      limit ?
                      for update skip locked)
                    returning id, tipo, organizacion_id, ticket_id, ocurrido_en, intentos
                    """,
                    (fila, numero) ->
                        new Reclamado(
                            new EventoTicket(
                                fila.getObject("id", UUID.class),
                                TipoEvento.valueOf(fila.getString("tipo")),
                                new OrganizacionId(fila.getObject("organizacion_id", UUID.class)),
                                new TicketId(fila.getObject("ticket_id", UUID.class)),
                                fila.getTimestamp("ocurrido_en").toInstant()),
                            fila.getInt("intentos")),
                    SEGUNDOS_DE_RECLAMO,
                    maximo));
    return reclamados.stream()
        .sorted(Comparator.comparing((Reclamado r) -> r.evento().ocurridoEn()))
        .toList();
  }

  @Override
  public void marcarHecho(UUID eventoId) {
    jdbc.update(
        "update evento_outbox set estado = 'HECHO', reclamado_hasta = null where id = ?", eventoId);
  }

  @Override
  public void fallar(UUID eventoId, String error, boolean definitivo) {
    String recortado = error == null ? null : error.substring(0, Math.min(error.length(), 500));
    // Un fallo reintentable acorta el reclamo: tiene que vencer antes del siguiente barrido (cada
    // minuto) o el barrido no encontraria nada que reintentar.
    jdbc.update(
        """
        update evento_outbox
        set ultimo_error = ?,
            estado = case when ? then 'FALLIDO' else estado end,
            reclamado_hasta = case when ? then reclamado_hasta
                                   else now() + make_interval(secs => ?) end
        where id = ?
        """,
        recortado,
        definitivo,
        definitivo,
        SEGUNDOS_DE_REINTENTO,
        eventoId);
  }

  /** Rondas de reactivacion de un evento FALLIDO antes de dejarlo por perdido. */
  static final int MAXIMO_DE_RONDAS = 3;

  /**
   * Devuelve a PENDIENTE, con los intentos a cero, los eventos FALLIDO que aun no han gastado sus
   * rondas. Corre cada hora: un fallo pasajero del proveedor o de la base no deja el evento perdido.
   */
  public int reactivarFallidos() {
    return jdbc.update(
        """
        update evento_outbox
        set estado = 'PENDIENTE', intentos = 0, rondas = rondas + 1, reclamado_hasta = null
        where estado = 'FALLIDO' and rondas < ?
        """,
        MAXIMO_DE_RONDAS);
  }

  @Scheduled(fixedDelay = 3_600_000, initialDelay = 600_000)
  void reactivarFallidosCadaHora() {
    int reactivados = reactivarFallidos();
    if (reactivados > 0) {
      LOG.info("Outbox: {} eventos fallidos vuelven a pendiente", reactivados);
    }
  }

  @Override
  public boolean yaProcesado(String consumidor, UUID eventoId) {
    Integer filas =
        jdbc.queryForObject(
            "select count(*) from evento_procesado where consumidor = ? and evento_id = ?",
            Integer.class,
            consumidor,
            eventoId);
    return filas != null && filas > 0;
  }

  @Override
  public void registrar(String consumidor, UUID eventoId) {
    jdbc.update(
        """
        insert into evento_procesado (consumidor, evento_id, procesado_en) values (?, ?, ?)
        on conflict do nothing
        """,
        consumidor,
        eventoId,
        Timestamp.from(Instant.now()));
  }

  @Override
  public int pendientes() {
    Integer filas =
        jdbc.queryForObject(
            "select count(*) from evento_outbox where estado = 'PENDIENTE'", Integer.class);
    return filas == null ? 0 : filas;
  }
}
