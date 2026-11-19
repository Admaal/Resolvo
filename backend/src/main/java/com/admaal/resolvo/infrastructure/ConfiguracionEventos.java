package com.admaal.resolvo.infrastructure;

import com.admaal.resolvo.application.Articulos;
import com.admaal.resolvo.application.BaseConocimiento;
import com.admaal.resolvo.application.ConsumidorEventos;
import com.admaal.resolvo.application.DesindexarTicketReabierto;
import com.admaal.resolvo.application.Embebedor;
import com.admaal.resolvo.application.IndexarConocimiento;
import com.admaal.resolvo.application.IndexarTicketResuelto;
import com.admaal.resolvo.application.ProcesarEventos;
import com.admaal.resolvo.application.TicketRepositorio;
import com.admaal.resolvo.infrastructure.ai.IaDesactivada;
import com.admaal.resolvo.infrastructure.persistencia.OutboxJdbc;
import java.util.ArrayList;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

@Configuration
@EnableScheduling
public class ConfiguracionEventos {

  static final int MAX_INTENTOS = 5;
  static final int TAMANO_DE_LOTE = 20;

  /**
   * Desindexar no necesita IA y va siempre. Indexar solo si hay proveedor de embeddings: sin el
   * (perfil gemini apagado) el evento se da por atendido por falta de consumidor y el arranque con IA
   * reindexa lo que falte.
   */
  @Bean
  ProcesarEventos procesarEventos(
      OutboxJdbc outbox,
      Embebedor embebedor,
      BaseConocimiento base,
      Articulos articulos,
      TicketRepositorio tickets,
      @Value("${resolvo.eventos.max-intentos:" + MAX_INTENTOS + "}") int maxIntentos) {
    List<ConsumidorEventos> consumidores = new ArrayList<>();
    consumidores.add(new DesindexarTicketReabierto(base, tickets));
    if (!(embebedor instanceof IaDesactivada)) {
      consumidores.add(
          new IndexarTicketResuelto(
              new IndexarConocimiento(embebedor, base, articulos, tickets), tickets));
    }
    return new ProcesarEventos(outbox, outbox, consumidores, maxIntentos, TAMANO_DE_LOTE);
  }
}
