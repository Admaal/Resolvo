package com.admaal.resolvo.infrastructure;

import com.admaal.resolvo.application.BandejaEventos;
import com.admaal.resolvo.application.ProcesarEventos;
import com.admaal.resolvo.infrastructure.persistencia.OutboxJdbc.EventoPublicado;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Despierta al procesador de eventos sin un poller continuo: una consulta cada pocos segundos
 * mantendria Neon despierto las 24 horas. Se procesa al confirmarse la transaccion que publico, al
 * arrancar (por si quedaron eventos de antes) y, cada minuto, solo si el ultimo pase dejo algo
 * pendiente (un reintento con espera o un evento reclamado por un proceso que murio: su reclamo
 * vence despues del arranque). En segundo plano y con un solo hilo: no retrasa la peticion ni se pisa consigo mismo.
 * Con {@code resolvo.ia.sincrona} (tests) no se dispara solo: el test llama a {@link #procesar()}.
 */
@Component
public class ProcesadorDeEventos {

  private static final Logger LOG = LoggerFactory.getLogger(ProcesadorDeEventos.class);

  private final ProcesarEventos proceso;
  private final BandejaEventos bandeja;
  private final Executor ejecutor;
  private final boolean automatico;
  // Verdadero mientras quede algo pendiente: reintentos con espera o eventos reclamados por un
  // proceso que murio. Empieza en verdadero para que el primer barrido tras arrancar mire la bandeja.
  private final AtomicBoolean hayPendientes = new AtomicBoolean(true);

  public ProcesadorDeEventos(
      ProcesarEventos proceso,
      BandejaEventos bandeja,
      @Qualifier("indexadorEjecutor") Executor ejecutor,
      @Value("${resolvo.ia.sincrona:false}") boolean sincrona) {
    this.proceso = proceso;
    this.bandeja = bandeja;
    this.ejecutor = ejecutor;
    this.automatico = !sincrona;
  }

  @TransactionalEventListener(fallbackExecution = true)
  void alPublicar(EventoPublicado aviso) {
    despertar();
  }

  @EventListener(ApplicationReadyEvent.class)
  void alArrancar() {
    despertar();
  }

  @Scheduled(fixedDelay = 60_000, initialDelay = 60_000)
  void barrer() {
    if (hayPendientes.get()) {
      despertar();
    }
  }

  private void despertar() {
    if (automatico) {
      ejecutor.execute(this::procesar);
    }
  }

  /** Atiende todo lo pendiente. Un solo pase a la vez; un error inesperado queda en el log. */
  public synchronized ProcesarEventos.Resultado procesar() {
    try {
      ProcesarEventos.Resultado resultado = proceso.ejecutar();
      hayPendientes.set(bandeja.pendientes() > 0);
      if (resultado.fallidos() > 0) {
        LOG.warn("{} eventos agotaron sus intentos; ver evento_outbox", resultado.fallidos());
      }
      return resultado;
    } catch (RuntimeException error) {
      hayPendientes.set(true);
      LOG.warn("No se pudo procesar la bandeja de eventos: {}", error.getMessage());
      return new ProcesarEventos.Resultado(0, 0, 0);
    }
  }
}
