package com.admaal.resolvo.infrastructure;

import static org.assertj.core.api.Assertions.assertThat;

import com.admaal.resolvo.application.BandejaEventos;
import com.admaal.resolvo.application.ConsumidorEventos;
import com.admaal.resolvo.application.ProcesarEventos;
import com.admaal.resolvo.application.soporte.BandejaEnMemoria;
import com.admaal.resolvo.application.soporte.ProcesadosEnMemoria;
import com.admaal.resolvo.domain.EventoTicket;
import com.admaal.resolvo.domain.OrganizacionId;
import com.admaal.resolvo.domain.TicketId;
import com.admaal.resolvo.domain.TipoEvento;
import com.admaal.resolvo.infrastructure.persistencia.OutboxJdbc.EventoPublicado;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executor;
import org.junit.jupiter.api.Test;

/** El disparo del procesador: cuando se despierta y cuando el barrido sigue vivo. */
class ProcesadorDeEventosTest {

  private final BandejaEnMemoria bandeja = new BandejaEnMemoria();
  private final List<Runnable> tareas = new ArrayList<>();
  private final Executor ejecutor = tareas::add;
  private boolean falla;

  private final ConsumidorEventos consumidor =
      new ConsumidorEventos() {
        @Override
        public String nombre() {
          return "c";
        }

        @Override
        public boolean admite(TipoEvento tipo) {
          return true;
        }

        @Override
        public void consumir(EventoTicket evento) {
          if (falla) {
            throw new IllegalStateException("boom");
          }
        }
      };

  private ProcesadorDeEventos procesador(boolean sincrona) {
    return procesador(bandeja, sincrona);
  }

  private ProcesadorDeEventos procesador(BandejaEventos laBandeja, boolean sincrona) {
    ProcesarEventos proceso =
        new ProcesarEventos(laBandeja, new ProcesadosEnMemoria(), List.of(consumidor), 5, 10);
    return new ProcesadorDeEventos(proceso, laBandeja, ejecutor, sincrona);
  }

  private EventoTicket evento() {
    EventoTicket evento =
        EventoTicket.nuevo(
            TipoEvento.TICKET_RESUELTO,
            new OrganizacionId(UUID.randomUUID()),
            new TicketId(UUID.randomUUID()),
            Instant.parse("2026-11-19T10:00:00Z"));
    bandeja.agregar(evento);
    return evento;
  }

  private void ejecutarTareas() {
    List<Runnable> pendientes = new ArrayList<>(tareas);
    tareas.clear();
    pendientes.forEach(Runnable::run);
  }

  @Test
  void alPublicarYAlArrancarSeDespiertaElProcesadorEnSegundoPlano() {
    ProcesadorDeEventos procesador = procesador(false);

    procesador.alPublicar(new EventoPublicado(UUID.randomUUID()));
    procesador.alArrancar();

    assertThat(tareas).hasSize(2);
  }

  @Test
  void enModoSincronoNoSeDisparaSoloYElTestLlamaAProcesar() {
    ProcesadorDeEventos procesador = procesador(true);

    procesador.alPublicar(new EventoPublicado(UUID.randomUUID()));
    procesador.alArrancar();
    procesador.barrer();

    assertThat(tareas).isEmpty();
  }

  @Test
  void unEventoQueFallaSeReintentaEnElSiguienteBarridoCuandoVenceElReclamo() {
    ProcesadorDeEventos procesador = procesador(false);
    falla = true;
    EventoTicket evento = evento();

    procesador.procesar();
    procesador.barrer();
    assertThat(tareas).hasSize(1);
    ejecutarTareas();
    assertThat(bandeja.estadoDe(evento.id())).isEqualTo(BandejaEnMemoria.Estado.PENDIENTE);

    falla = false;
    bandeja.vencerReclamos();
    procesador.barrer();
    ejecutarTareas();

    assertThat(bandeja.estadoDe(evento.id())).isEqualTo(BandejaEnMemoria.Estado.HECHO);
  }

  @Test
  void siUnProcesadorCayoConElEventoReclamadoElBarridoSigueVivoHastaQueVenceElReclamo() {
    ProcesadorDeEventos procesador = procesador(false);
    EventoTicket evento = evento();
    bandeja.reclamar(10); // el proceso anterior lo reclamo y murio sin terminar

    procesador.procesar();
    procesador.barrer();
    assertThat(tareas).hasSize(1);
    tareas.clear();

    bandeja.vencerReclamos();
    procesador.procesar();

    assertThat(bandeja.estadoDe(evento.id())).isEqualTo(BandejaEnMemoria.Estado.HECHO);
  }

  @Test
  void sinNadaPendienteElBarridoNoDespiertaNiConsulta() {
    ProcesadorDeEventos procesador = procesador(false);
    evento();
    procesador.procesar();

    procesador.barrer();

    assertThat(tareas).isEmpty();
  }

  @Test
  void unErrorInesperadoDeLaBandejaNoSePierdeYElBarridoLoReintenta() {
    BandejaEventos rota =
        new BandejaEventos() {
          @Override
          public List<Reclamado> reclamar(int maximo) {
            throw new IllegalStateException("base de datos caida");
          }

          @Override
          public void marcarHecho(UUID eventoId) {}

          @Override
          public void fallar(UUID eventoId, String error, boolean definitivo) {}

          @Override
          public int pendientes() {
            return 0;
          }
        };
    ProcesadorDeEventos procesador = procesador(rota, false);

    ProcesarEventos.Resultado resultado = procesador.procesar();
    procesador.barrer();

    assertThat(resultado).isEqualTo(new ProcesarEventos.Resultado(0, 0, 0));
    assertThat(tareas).hasSize(1);
  }
}
