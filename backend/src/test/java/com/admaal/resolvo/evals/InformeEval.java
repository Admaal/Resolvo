package com.admaal.resolvo.evals;

import com.admaal.resolvo.evals.ResultadoEval.Sentido;
import java.util.List;
import java.util.Locale;

/** El informe en Markdown que va al resumen del job de CI. Nunca incluye la clave del proveedor. */
public final class InformeEval {

  private static final Locale ES = Locale.of("es", "ES");

  private InformeEval() {}

  public static boolean aprobado(List<ResultadoEval> resultados) {
    return !resultados.isEmpty() && resultados.stream().allMatch(ResultadoEval::cumple);
  }

  /**
   * @param clave la clave del proveedor, para tacharla de todo el texto: los errores del SDK pueden
   *     traerla en la URL. Si es nula o vacia no se tacha nada.
   */
  public static String markdown(List<ResultadoEval> resultados, List<String> notas, String clave) {
    StringBuilder informe = new StringBuilder();
    informe.append("## Evals de IA: ").append(aprobado(resultados) ? "APROBADO" : "SUSPENDIDO").append("\n\n");
    informe.append("| Metrica | Valor | Umbral | Veredicto |\n| --- | --- | --- | --- |\n");
    for (ResultadoEval resultado : resultados) {
      informe
          .append("| ")
          .append(resultado.nombre())
          .append(" | ")
          .append(formatear(resultado.valor(), resultado.unidad()))
          .append(" | ")
          .append(resultado.sentido() == Sentido.MINIMO ? ">= " : "<= ")
          .append(formatear(resultado.umbral(), resultado.unidad()))
          .append(" | ")
          .append(resultado.cumple() ? "OK" : "FALLA")
          .append(" |\n");
    }
    if (!notas.isEmpty()) {
      informe.append("\n");
      notas.forEach(nota -> informe.append("- ").append(nota).append('\n'));
    }
    return ocultar(informe.toString(), clave);
  }

  private static String formatear(double valor, String unidad) {
    String sufijo = unidad == null || unidad.isBlank() ? "" : " " + unidad;
    if ("%".equals(unidad)) {
      return String.format(ES, "%.1f", valor * 100) + sufijo;
    }
    boolean entero = valor == Math.rint(valor);
    return (entero ? String.format(ES, "%d", (long) valor) : String.format(ES, "%.1f", valor)) + sufijo;
  }

  private static String ocultar(String texto, String clave) {
    return clave == null || clave.isBlank() ? texto : texto.replace(clave.strip(), "***");
  }
}
