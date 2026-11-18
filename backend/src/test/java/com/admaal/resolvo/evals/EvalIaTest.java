package com.admaal.resolvo.evals;

import static org.assertj.core.api.Assertions.assertThat;

import com.admaal.resolvo.application.BaseConocimiento;
import com.admaal.resolvo.application.BorradorConCitas;
import com.admaal.resolvo.application.CatalogoDeCategorias;
import com.admaal.resolvo.application.ClasificacionPropuesta;
import com.admaal.resolvo.application.ClasificadorTickets;
import com.admaal.resolvo.application.Embebedor;
import com.admaal.resolvo.application.IaNoDisponible;
import com.admaal.resolvo.application.RedactorRespuestas;
import com.admaal.resolvo.bootstrap.ResolvoApplication;
import com.admaal.resolvo.domain.Categoria;
import com.admaal.resolvo.domain.Embedding;
import com.admaal.resolvo.domain.Fragmento;
import com.admaal.resolvo.domain.FragmentoId;
import com.admaal.resolvo.domain.OrganizacionId;
import com.admaal.resolvo.domain.Prioridad;
import com.admaal.resolvo.domain.Rol;
import com.admaal.resolvo.domain.Ticket;
import com.admaal.resolvo.domain.TicketId;
import com.admaal.resolvo.domain.UsuarioId;
import com.admaal.resolvo.evals.ResultadoEval.Sentido;
import com.admaal.resolvo.infrastructure.AplicacionDeConocimiento;
import com.admaal.resolvo.infrastructure.PostgresDePrueba;
import com.admaal.resolvo.infrastructure.ai.LlamadorGemini;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

/**
 * Evals contra Gemini real: calidad, coste y latencia de la clasificacion y del RAG. No corren en
 * {@code gradlew test} (etiqueta "eval"): se lanzan con {@code gradlew evals}, con la clave en
 * RESOLVO_GEMINI_API_KEY. Sin clave el perfil gemini no arranca y el test falla con un mensaje claro.
 */
@Tag("eval")
@SpringBootTest(classes = ResolvoApplication.class)
@ActiveProfiles("gemini")
class EvalIaTest {

  private static final OrganizacionId ACME =
      new OrganizacionId(UUID.fromString("11111111-1111-1111-1111-111111111111"));
  private static final UsuarioId AUTOR =
      new UsuarioId(UUID.fromString("22222222-2222-2222-2222-222222222221"));
  private static final int K = 5;

  @DynamicPropertySource
  static void postgres(DynamicPropertyRegistry registro) {
    PostgresDePrueba.registrar(registro);
  }

  @Autowired ClasificadorTickets clasificador;
  @Autowired RedactorRespuestas redactor;
  @Autowired Embebedor embebedor;
  @Autowired BaseConocimiento base;
  @Autowired CatalogoDeCategorias catalogo;
  @Autowired AplicacionDeConocimiento conocimiento;
  @Autowired LlamadorGemini llamador;
  @Autowired JdbcTemplate jdbc;

  @Value("${resolvo.rag.distancia-maxima:2.0}")
  double distanciaMaxima;

  @Value("${resolvo.evals.pausa-ms:4500}")
  long pausaMs;

  private final List<LlamadorGemini.Respuesta> llamadas = Collections.synchronizedList(new ArrayList<>());
  private final List<String> notas = new ArrayList<>();

  @Test
  void clasificacionYRagCumplenSusUmbrales() throws Exception {
    Umbrales umbrales = Umbrales.cargar();
    llamador.observar(llamadas::add);
    llamador.comprobar();
    List<ResultadoEval> resultados = new ArrayList<>();

    evaluarClasificacion(umbrales, resultados);
    evaluarRag(umbrales, resultados);
    evaluarCosteYLatencia(umbrales, resultados);

    String clave = System.getenv("RESOLVO_GEMINI_API_KEY");
    String informe = InformeEval.markdown(resultados, notas, clave);
    System.out.println(informe);
    Path destino = Path.of("build", "reports", "evals", "informe.md");
    Files.createDirectories(destino.getParent());
    Files.writeString(destino, informe);

    assertThat(InformeEval.aprobado(resultados)).as(informe).isTrue();
  }

  private void evaluarClasificacion(Umbrales umbrales, List<ResultadoEval> resultados)
      throws InterruptedException {
    List<Categoria> categorias = catalogo.deLaOrganizacion(ACME);
    Map<String, Categoria> porNombre = new HashMap<>();
    categorias.forEach(categoria -> porNombre.put(categoria.nombre().toLowerCase(), categoria));
    List<CasoClasificacion> casos = CargadorDeCasos.clasificacion();
    int validas = 0;
    int categoriaOk = 0;
    int prioridadOk = 0;
    Set<String> errores = new LinkedHashSet<>();
    List<String> desaciertos = new ArrayList<>();
    for (CasoClasificacion caso : casos) {
      try {
        ClasificacionPropuesta propuesta =
            clasificador.clasificar(ticket(caso.id(), caso.asunto(), caso.cuerpo()), categorias);
        validas++;
        if (propuesta.categoria() != null
            && propuesta.categoria().strip().equalsIgnoreCase(caso.categoria())) {
          categoriaOk++;
        }
        if (propuesta.prioridad() == caso.prioridad()) {
          prioridadOk++;
        } else {
          desaciertos.add(
              "%s: esperada %s, propuso %s (%s)"
                  .formatted(caso.id(), caso.prioridad(), propuesta.prioridad(), propuesta.motivo()));
        }
      } catch (IaNoDisponible fallo) {
        errores.add(caso.id() + ": " + fallo.getMessage());
      }
      Thread.sleep(pausaMs);
    }
    resultados.add(
        new ResultadoEval(
            "Clasificacion: respuestas validas",
            MetricasEval.proporcion(validas, casos.size()),
            umbrales.respuestaValida(),
            Sentido.MINIMO,
            "%"));
    resultados.add(
        new ResultadoEval(
            "Clasificacion: acierto de categoria",
            MetricasEval.proporcion(categoriaOk, casos.size()),
            umbrales.categoriaAcierto(),
            Sentido.MINIMO,
            "%"));
    resultados.add(
        new ResultadoEval(
            "Clasificacion: prioridad exacta",
            MetricasEval.proporcion(prioridadOk, casos.size()),
            umbrales.prioridadExacta(),
            Sentido.MINIMO,
            "%"));
    notas.add("Clasificacion: %d casos, categorias %s".formatted(casos.size(), porNombre.keySet()));
    errores.stream().limit(10).forEach(error -> notas.add("Error de clasificacion " + error));
    desaciertos.forEach(desacierto -> notas.add("Prioridad distinta " + desacierto));
  }

  private void evaluarRag(Umbrales umbrales, List<ResultadoEval> resultados)
      throws InterruptedException {
    conocimiento.reindexar(ACME, Rol.ADMIN);
    Map<String, UUID> articulos = new HashMap<>();
    jdbc.query(
        "select id, titulo from articulo where organizacion_id = ?",
        fila -> {
          articulos.put(fila.getString("titulo"), fila.getObject("id", UUID.class));
        },
        ACME.valor());
    List<CasoRag> casos = CargadorDeCasos.rag();
    int conRespuesta = 0;
    int recall = 0;
    int citan = 0;
    int sinRespuesta = 0;
    int sinRespuestaAcertada = 0;
    List<Double> distanciasConRespuesta = new ArrayList<>();
    List<Double> distanciasSinRespuesta = new ArrayList<>();
    Set<String> errores = new LinkedHashSet<>();
    for (CasoRag caso : casos) {
      // Se cuenta antes de llamar al proveedor: un caso que falla es un fallo, no un caso menos.
      if (caso.tieneRespuesta()) {
        conRespuesta++;
      } else {
        sinRespuesta++;
      }
      Ticket ticket = ticket(caso.id(), caso.asunto(), caso.cuerpo());
      try {
        Embedding consulta = embebedor.embeber(caso.asunto() + "\n" + caso.cuerpo());
        List<Double> cercanos = distanciasCercanas(consulta);
        double mejor = cercanos.isEmpty() ? 2.0 : cercanos.get(0);
        List<Fragmento> fragmentos =
            base.buscar(ACME, consulta, K, UUID.randomUUID(), distanciaMaxima);
        if (caso.tieneRespuesta()) {
          distanciasConRespuesta.add(mejor);
          UUID esperado = articulos.get(caso.articulo());
          if (fuentesCercanas(consulta).contains(esperado)) {
            recall++;
          }
          Thread.sleep(pausaMs);
          if (!fragmentos.isEmpty() && citaLaFuente(ticket, fragmentos, esperado)) {
            citan++;
          }
        } else {
          distanciasSinRespuesta.add(mejor);
          if (fragmentos.isEmpty()) {
            sinRespuestaAcertada++;
          }
        }
      } catch (IaNoDisponible fallo) {
        errores.add(caso.id() + ": " + fallo.getMessage());
      }
      Thread.sleep(pausaMs);
    }
    resultados.add(
        new ResultadoEval(
            "RAG: recall@5 del articulo esperado",
            MetricasEval.proporcion(recall, conRespuesta),
            umbrales.recallAtCinco(),
            Sentido.MINIMO,
            "%"));
    resultados.add(
        new ResultadoEval(
            "RAG: sin respuesta devuelve sin fuentes",
            MetricasEval.proporcion(sinRespuestaAcertada, sinRespuesta),
            umbrales.sinRespuestaAcierto(),
            Sentido.MINIMO,
            "%"));
    resultados.add(
        new ResultadoEval(
            "RAG: el borrador cita la fuente esperada",
            MetricasEval.proporcion(citan, conRespuesta),
            umbrales.citaLaFuenteEsperada(),
            Sentido.MINIMO,
            "%"));
    notas.add("RAG: umbral de distancia configurado %s".formatted(distanciaMaxima));
    notas.add(
        MetricasEval.umbralRecomendado(distanciasConRespuesta, distanciasSinRespuesta)
            .map(umbral -> "RAG: umbral de distancia recomendado %.3f (separa las consultas con y sin respuesta)".formatted(umbral))
            .orElse("RAG: ningun umbral de distancia separa las consultas con y sin respuesta"));
    notas.add("RAG: mejor distancia con respuesta " + distanciasConRespuesta);
    notas.add("RAG: mejor distancia sin respuesta " + distanciasSinRespuesta);
    errores.stream().limit(10).forEach(error -> notas.add("Error de RAG " + error));
  }

  private void evaluarCosteYLatencia(Umbrales umbrales, List<ResultadoEval> resultados) {
    List<LlamadorGemini.Respuesta> vistas = List.copyOf(llamadas);
    double tokensMedios =
        MetricasEval.media(vistas.stream().map(r -> r.tokensEntrada() + r.tokensSalida()).toList());
    long p95 = MetricasEval.p95(vistas.stream().map(LlamadorGemini.Respuesta::milisegundos).toList());
    resultados.add(
        new ResultadoEval(
            "Coste: tokens medios por llamada",
            tokensMedios,
            umbrales.tokensMediosPorLlamada(),
            Sentido.MAXIMO,
            "tokens"));
    resultados.add(
        new ResultadoEval(
            "Latencia: p95 por llamada", p95, umbrales.p95Milisegundos(), Sentido.MAXIMO, "ms"));
    int entrada = vistas.stream().mapToInt(LlamadorGemini.Respuesta::tokensEntrada).sum();
    int salida = vistas.stream().mapToInt(LlamadorGemini.Respuesta::tokensSalida).sum();
    notas.add(
        "Llamadas de redaccion y clasificacion: %d; tokens de entrada %d y de salida %d; modelos %s"
            .formatted(
                vistas.size(),
                entrada,
                salida,
                vistas.stream().map(LlamadorGemini.Respuesta::modelo).distinct().toList()));
    notas.add("Coste en euros: free tier (0 EUR). Sin tabla de precios de lista versionada.");
  }

  private boolean citaLaFuente(Ticket ticket, List<Fragmento> fragmentos, UUID esperado) {
    try {
      BorradorConCitas borrador = redactor.redactar(ticket, fragmentos);
      Set<FragmentoId> citados = new LinkedHashSet<>(borrador.citas());
      return fragmentos.stream()
          .anyMatch(f -> citados.contains(f.id()) && f.fuenteId().equals(esperado));
    } catch (IaNoDisponible fallo) {
      Throwable causa = fallo.getCause();
      notas.add(
          "Error al redactar %s: %s (%s)"
              .formatted(
                  ticket.codigo(),
                  fallo.getMessage(),
                  causa == null
                      ? "sin excepcion de origen: la respuesta del modelo no era un JSON util"
                      : causa.getClass().getSimpleName() + ": " + causa.getMessage()));
      return false;
    }
  }

  /** Distancia coseno de los K fragmentos mas cercanos, de menor a mayor. */
  private List<Double> distanciasCercanas(Embedding consulta) {
    return jdbc.query(
        """
        select (embedding <=> ?::vector) + 0 as distancia
        from fragmento where organizacion_id = ? order by distancia limit ?
        """,
        (fila, numero) -> fila.getDouble("distancia"),
        literal(consulta),
        ACME.valor(),
        K);
  }

  private Set<UUID> fuentesCercanas(Embedding consulta) {
    return new LinkedHashSet<>(
        jdbc.queryForList(
            """
            select fuente_id from fragmento
            where organizacion_id = ? order by (embedding <=> ?::vector) + 0 limit ?
            """,
            UUID.class,
            ACME.valor(),
            literal(consulta),
            K));
  }

  private Ticket ticket(String id, String asunto, String cuerpo) {
    Instant ahora = Instant.parse("2026-11-16T10:00:00Z");
    Categoria categoria = catalogo.deLaOrganizacion(ACME).get(0);
    return Ticket.abrir(
        TicketId.nuevo(),
        ACME,
        AUTOR,
        categoria.id(),
        Prioridad.MEDIA,
        "EV-" + id,
        asunto,
        cuerpo,
        ahora,
        ahora.plusSeconds(3600));
  }

  private static String literal(Embedding embedding) {
    StringBuilder texto = new StringBuilder("[");
    float[] valores = embedding.valores();
    for (int i = 0; i < valores.length; i++) {
      if (i > 0) {
        texto.append(',');
      }
      texto.append(valores[i]);
    }
    return texto.append(']').toString();
  }
}
