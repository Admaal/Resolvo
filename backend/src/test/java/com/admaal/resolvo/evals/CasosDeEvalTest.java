package com.admaal.resolvo.evals;

import static org.assertj.core.api.Assertions.assertThat;

import com.admaal.resolvo.domain.Prioridad;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

/** Que los datasets de evals esten bien formados: un dataset roto haria un eval que no mide nada. */
class CasosDeEvalTest {

  private final List<CasoClasificacion> clasificacion = CargadorDeCasos.clasificacion();
  private final List<CasoRag> rag = CargadorDeCasos.rag();

  @Test
  void clasificacionTieneCasosSuficientesYSinIdsRepetidos() {
    assertThat(clasificacion).hasSizeGreaterThanOrEqualTo(30);
    assertThat(clasificacion.stream().map(CasoClasificacion::id)).doesNotHaveDuplicates();
  }

  @Test
  void clasificacionSoloUsaLasCategoriasDeLaSemilla() {
    assertThat(clasificacion)
        .extracting(CasoClasificacion::categoria)
        .containsOnly("Incidencia", "Consulta");
  }

  @Test
  void clasificacionNoEstaDegenerada_cadaPrioridadYCategoriaTieneVariosCasos() {
    Map<Prioridad, Long> porPrioridad =
        clasificacion.stream()
            .collect(Collectors.groupingBy(CasoClasificacion::prioridad, Collectors.counting()));
    Map<String, Long> porCategoria =
        clasificacion.stream()
            .collect(Collectors.groupingBy(CasoClasificacion::categoria, Collectors.counting()));

    for (Prioridad prioridad : Prioridad.values()) {
      assertThat(porPrioridad.getOrDefault(prioridad, 0L)).as(prioridad.name()).isGreaterThanOrEqualTo(4);
    }
    assertThat(porCategoria.get("Incidencia")).isGreaterThanOrEqualTo(10);
    assertThat(porCategoria.get("Consulta")).isGreaterThanOrEqualTo(10);
  }

  @Test
  void clasificacionTieneAsuntoYCuerpoDentroDeLosLimitesDelDominio() {
    assertThat(clasificacion)
        .allSatisfy(
            caso -> {
              assertThat(caso.asunto()).isNotBlank().hasSizeLessThanOrEqualTo(200);
              assertThat(caso.cuerpo()).isNotBlank().hasSizeLessThanOrEqualTo(8000);
            });
  }

  @Test
  void ragTieneCasosConRespuestaYSinRespuestaYSinIdsRepetidos() {
    assertThat(rag.stream().map(CasoRag::id)).doesNotHaveDuplicates();
    assertThat(rag.stream().filter(CasoRag::tieneRespuesta).count()).isGreaterThanOrEqualTo(14);
    assertThat(rag.stream().filter(caso -> !caso.tieneRespuesta()).count()).isGreaterThanOrEqualTo(5);
  }

  @Test
  void ragSoloEsperaArticulosQueExistenEnLaSemilla() throws IOException {
    String semilla =
        new String(
            new ClassPathResource("db/migration/V5__semilla_articulos.sql").getInputStream().readAllBytes(),
            StandardCharsets.UTF_8);

    assertThat(rag.stream().map(CasoRag::articulo).filter(java.util.Objects::nonNull).distinct())
        .allSatisfy(titulo -> assertThat(semilla).contains("'" + titulo + "'"));
  }

  @Test
  void ragTieneAlMenosCuarentaCasos() {
    assertThat(rag).hasSizeGreaterThanOrEqualTo(40);
    assertThat(rag.stream().filter(caso -> !caso.tieneRespuesta()).count()).isGreaterThanOrEqualTo(10);
  }

  @Test
  void ragMideTambienLosTicketsResueltosComoFuente() throws IOException {
    String semilla =
        new String(
            new ClassPathResource("db/migration/V3__semilla_demo.sql").getInputStream().readAllBytes(),
            StandardCharsets.UTF_8);
    List<CasoRag> conTicket = rag.stream().filter(caso -> caso.ticket() != null).toList();

    assertThat(conTicket).hasSizeGreaterThanOrEqualTo(4);
    assertThat(conTicket).allSatisfy(caso -> assertThat(caso.articulo()).isNull());
    assertThat(conTicket.stream().map(CasoRag::ticket).distinct())
        .allSatisfy(codigo -> assertThat(semilla).contains("'" + codigo + "'"))
        .containsExactlyInAnyOrder("RS-4", "RS-5");
  }

  @Test
  void losCasosConTicketAceptanElArticuloQueDaLaMismaRespuesta() throws IOException {
    String semilla =
        new String(
            new ClassPathResource("db/migration/V5__semilla_articulos.sql").getInputStream().readAllBytes(),
            StandardCharsets.UTF_8);
    Map<String, String> equivalente =
        Map.of(
            "RS-4", "Cambiar los datos de facturacion",
            "RS-5", "Notificaciones por correo duplicadas");

    assertThat(rag.stream().filter(caso -> caso.ticket() != null))
        .allSatisfy(
            caso -> {
              assertThat(caso.tambien()).as(caso.id()).isEqualTo(equivalente.get(caso.ticket()));
              assertThat(semilla).contains("'" + caso.tambien() + "'");
            });
    assertThat(rag.stream().filter(caso -> caso.ticket() == null).map(CasoRag::tambien))
        .containsOnlyNulls();
  }

  @Test
  void ragCubreVariosArticulosYNoSoloUno() {
    Map<String, Long> porArticulo =
        rag.stream()
            .filter(caso -> caso.articulo() != null)
            .collect(Collectors.groupingBy(CasoRag::articulo, Collectors.counting()));

    assertThat(porArticulo).hasSizeGreaterThanOrEqualTo(6);
  }

  @Test
  void ningunCasoRepiteElMismoTextoQueOtro() {
    Set<String> textos =
        rag.stream().map(caso -> caso.asunto() + "|" + caso.cuerpo()).collect(Collectors.toSet());

    assertThat(textos).hasSize(rag.size());
    assertThat(
            clasificacion.stream()
                .collect(Collectors.toMap(c -> c.asunto() + "|" + c.cuerpo(), Function.identity(), (a, b) -> a)))
        .hasSize(clasificacion.size());
  }
}
