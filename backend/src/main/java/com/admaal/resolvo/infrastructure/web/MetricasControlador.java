package com.admaal.resolvo.infrastructure.web;

import com.admaal.resolvo.application.Reloj;
import com.admaal.resolvo.domain.ResumenLlamadas;
import com.admaal.resolvo.infrastructure.AplicacionDeMetricas;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class MetricasControlador {

  private static final Duration RANGO_POR_DEFECTO = Duration.ofDays(7);

  private final AplicacionDeMetricas aplicacion;
  private final Reloj reloj;

  public MetricasControlador(AplicacionDeMetricas aplicacion, Reloj reloj) {
    this.aplicacion = aplicacion;
    this.reloj = reloj;
  }

  record FilaRespuesta(
      String operacion,
      String modelo,
      int llamadas,
      int fallos,
      long tokensEntrada,
      long tokensSalida,
      double tokensMedios,
      long p50Ms,
      long p95Ms,
      BigDecimal costeUsd) {}

  record MetricasRespuesta(
      Instant desde,
      Instant hasta,
      int llamadas,
      int fallos,
      long tokensEntrada,
      long tokensSalida,
      BigDecimal costeUsd,
      String nota,
      List<FilaRespuesta> filas) {}

  /** Solo ADMIN, solo de su organizacion; el rango por defecto son los ultimos 7 dias. */
  @GetMapping("/api/metricas/ia")
  ResponseEntity<?> ia(
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant desde,
      @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant hasta) {
    Actor actor = Actor.actual();
    Instant fin = hasta == null ? reloj.ahora() : hasta;
    Instant inicio = desde == null ? fin.minus(RANGO_POR_DEFECTO) : desde;
    ResumenLlamadas resumen;
    try {
      resumen = aplicacion.resumen(actor.organizacionId(), actor.rol(), inicio, fin);
    } catch (IllegalArgumentException rangoAlReves) {
      return ResponseEntity.unprocessableEntity().body(Map.of("error", "rango-invalido"));
    }
    return ResponseEntity.ok(
        new MetricasRespuesta(
            inicio,
            fin,
            resumen.llamadas(),
            resumen.fallos(),
            resumen.tokensEntrada(),
            resumen.tokensSalida(),
            resumen.costeUsd(),
            "Coste en USD a precio de lista; nulo si algun modelo no tiene tarifa. El nivel gratuito cuesta 0.",
            resumen.filas().stream()
                .map(
                    fila ->
                        new FilaRespuesta(
                            fila.operacion(),
                            fila.modelo(),
                            fila.llamadas(),
                            fila.fallos(),
                            fila.tokensEntrada(),
                            fila.tokensSalida(),
                            fila.tokensMedios(),
                            fila.p50Ms(),
                            fila.p95Ms(),
                            fila.costeUsd()))
                .toList()));
  }
}
