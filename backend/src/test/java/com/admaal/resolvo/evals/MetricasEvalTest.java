package com.admaal.resolvo.evals;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.LongStream;
import org.junit.jupiter.api.Test;

class MetricasEvalTest {

  @Test
  void proporcionDivideAciertosEntreTotal() {
    assertThat(MetricasEval.proporcion(3, 4)).isEqualTo(0.75);
    assertThat(MetricasEval.proporcion(4, 4)).isEqualTo(1.0);
  }

  @Test
  void proporcionSinCasosEsCeroYNoUnaDivisionPorCero() {
    assertThat(MetricasEval.proporcion(0, 0)).isEqualTo(0.0);
  }

  @Test
  void p95ConPocasMuestras() {
    assertThat(MetricasEval.p95(List.of(7L))).isEqualTo(7);
    assertThat(MetricasEval.p95(List.of(3L, 9L))).isEqualTo(9);
  }

  @Test
  void p95SinMuestrasEsCero() {
    assertThat(MetricasEval.p95(List.of())).isZero();
  }

  @Test
  void p95EsElRangoMasCercanoDelNoventaYCincoPorCiento() {
    List<Long> cien = LongStream.rangeClosed(1, 100).boxed().toList();
    List<Long> veinte = LongStream.rangeClosed(1, 20).boxed().toList();

    assertThat(MetricasEval.p95(cien)).isEqualTo(95);
    assertThat(MetricasEval.p95(veinte)).isEqualTo(19);
  }

  @Test
  void p95NoDependeDelOrdenDeEntradaNiLoModifica() {
    List<Long> desordenadas = new ArrayList<>(List.of(50L, 10L, 40L, 20L, 30L));

    assertThat(MetricasEval.p95(desordenadas)).isEqualTo(50);
    assertThat(desordenadas).containsExactly(50L, 10L, 40L, 20L, 30L);
  }

  @Test
  void mediaDeEnterosYDeListaVacia() {
    assertThat(MetricasEval.media(List.of(10, 20, 60))).isEqualTo(30.0);
    assertThat(MetricasEval.media(List.of())).isZero();
  }

  @Test
  void umbralRecomendadoEsElPuntoMedioSiSeSeparan() {
    assertThat(MetricasEval.umbralRecomendado(List.of(0.2, 0.3), List.of(0.5, 0.7)))
        .hasValue(0.4);
  }

  @Test
  void umbralRecomendadoNoExisteSiSeSolapan() {
    assertThat(MetricasEval.umbralRecomendado(List.of(0.2, 0.6), List.of(0.5, 0.7))).isEmpty();
    assertThat(MetricasEval.umbralRecomendado(List.of(0.5), List.of(0.5))).isEmpty();
  }

  @Test
  void umbralRecomendadoSinMuestrasDeUnLadoNoExiste() {
    assertThat(MetricasEval.umbralRecomendado(List.of(), List.of(0.5))).isEmpty();
    assertThat(MetricasEval.umbralRecomendado(List.of(0.2), List.of())).isEmpty();
  }
}
