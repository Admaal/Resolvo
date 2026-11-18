package com.admaal.resolvo.evals;

import static org.assertj.core.api.Assertions.assertThat;

import com.admaal.resolvo.evals.ResultadoEval.Sentido;
import java.util.List;
import org.junit.jupiter.api.Test;

class InformeEvalTest {

  private static final ResultadoEval ACIERTO =
      new ResultadoEval("Acierto de categoria", 0.93, 0.9, Sentido.MINIMO, "%");
  private static final ResultadoEval LATENCIA =
      new ResultadoEval("Latencia p95", 6200, 8000, Sentido.MAXIMO, "ms");

  @Test
  void minimoCumpleSiLlegaAlUmbralIncluido() {
    assertThat(new ResultadoEval("a", 0.9, 0.9, Sentido.MINIMO, "%").cumple()).isTrue();
    assertThat(new ResultadoEval("a", 0.89, 0.9, Sentido.MINIMO, "%").cumple()).isFalse();
  }

  @Test
  void maximoCumpleSiNoLoSuperaIncluido() {
    assertThat(new ResultadoEval("a", 8000, 8000, Sentido.MAXIMO, "ms").cumple()).isTrue();
    assertThat(new ResultadoEval("a", 8001, 8000, Sentido.MAXIMO, "ms").cumple()).isFalse();
  }

  @Test
  void aprobadoExigeQueCumplaCadaUmbral() {
    ResultadoEval falla = new ResultadoEval("Recall", 0.5, 0.8, Sentido.MINIMO, "%");

    assertThat(InformeEval.aprobado(List.of(ACIERTO, LATENCIA))).isTrue();
    assertThat(InformeEval.aprobado(List.of(ACIERTO, falla, LATENCIA))).isFalse();
  }

  @Test
  void unInformeSinResultadosNoAprueba() {
    assertThat(InformeEval.aprobado(List.of())).isFalse();
  }

  @Test
  void elMarkdownTrae_unaFilaPorMetricaConValorUmbralYVeredicto() {
    String informe = InformeEval.markdown(List.of(ACIERTO, LATENCIA), List.of(), null);

    assertThat(informe)
        .contains("| Acierto de categoria | 93,0 % | >= 90,0 % | OK |")
        .contains("| Latencia p95 | 6200 ms | <= 8000 ms | OK |")
        .contains("APROBADO");
  }

  @Test
  void unaMetricaQueFallaSeMarcaYElInformeSuspende() {
    ResultadoEval falla = new ResultadoEval("Recall", 0.5, 0.8, Sentido.MINIMO, "%");

    String informe = InformeEval.markdown(List.of(ACIERTO, falla), List.of(), null);

    assertThat(informe).contains("| Recall | 50,0 % | >= 80,0 % | FALLA |").contains("SUSPENDIDO");
  }

  @Test
  void lasNotasSalenEnElInforme() {
    String informe =
        InformeEval.markdown(List.of(ACIERTO), List.of("Modelo: gemini-x", "Umbral recomendado: 0,41"), null);

    assertThat(informe).contains("Modelo: gemini-x").contains("Umbral recomendado: 0,41");
  }

  @Test
  void elInformeNoIncluyeLaClave() {
    String clave = "AIzaSyClaveSecretaDePrueba123";
    ResultadoEval conClave =
        new ResultadoEval("Fallo con " + clave, 1, 1, Sentido.MINIMO, "");

    String informe =
        InformeEval.markdown(
            List.of(conClave),
            List.of("Error: https://api.google.test/v1?key=" + clave + " devolvio 403"),
            clave);

    assertThat(informe).doesNotContain(clave).contains("***");
  }

  @Test
  void sinClaveConocidaElTextoSeDejaTalCual() {
    String informe = InformeEval.markdown(List.of(ACIERTO), List.of("nota normal"), "  ");

    assertThat(informe).contains("nota normal").doesNotContain("***");
  }
}
