package com.admaal.resolvo.infrastructure.persistencia;

import static org.assertj.core.api.Assertions.assertThat;

import com.admaal.resolvo.application.FragmentoIndexado;
import com.admaal.resolvo.bootstrap.ResolvoApplication;
import com.admaal.resolvo.domain.Embedding;
import com.admaal.resolvo.domain.Fragmento;
import com.admaal.resolvo.domain.FragmentoId;
import com.admaal.resolvo.domain.FuenteConocimiento;
import com.admaal.resolvo.domain.OrganizacionId;
import com.admaal.resolvo.infrastructure.PostgresDePrueba;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@Tag("full")
@SpringBootTest(classes = ResolvoApplication.class)
class BaseConocimientoPgvectorTest {

  private static final OrganizacionId ACME =
      new OrganizacionId(UUID.fromString("11111111-1111-1111-1111-111111111111"));

  @DynamicPropertySource
  static void postgres(DynamicPropertyRegistry registro) {
    PostgresDePrueba.registrar(registro);
  }

  @Autowired BaseConocimientoPgvector base;
  @Autowired JdbcTemplate jdbc;

  @BeforeEach
  @AfterEach
  void limpiar() {
    jdbc.update("delete from fragmento");
  }

  private OrganizacionId otraOrganizacion() {
    UUID id = UUID.randomUUID();
    jdbc.update("insert into organizacion (id, nombre) values (?, 'Otra')", id);
    return new OrganizacionId(id);
  }

  /** Vector unitario sobre un eje, con una pizca de otro eje para controlar la distancia. */
  private static Embedding eje(int principal, int secundario, float peso) {
    float[] valores = new float[Embedding.DIMENSION];
    valores[principal] = 1f;
    valores[secundario] += peso;
    return new Embedding(valores);
  }

  private FragmentoIndexado indexado(
      OrganizacionId org, UUID fuenteId, String titulo, Embedding embedding) {
    return new FragmentoIndexado(
        new Fragmento(
            FragmentoId.nuevo(), org, FuenteConocimiento.ARTICULO, fuenteId, titulo, "texto " + titulo),
        embedding);
  }

  private UUID guardar(OrganizacionId org, String titulo, Embedding embedding) {
    UUID fuente = UUID.randomUUID();
    base.reemplazar(
        FuenteConocimiento.ARTICULO,
        fuente,
        "hash-" + titulo,
        "m",
        List.of(indexado(org, fuente, titulo, embedding)));
    return fuente;
  }

  @Test
  void buscaPorCosenoYFiltraOrganizacion() {
    OrganizacionId otra = otraOrganizacion();
    guardar(ACME, "cercano", eje(0, 5, 0.1f));
    guardar(ACME, "lejano", eje(1, 5, 0.1f));
    guardar(otra, "ajeno-identico", eje(0, 5, 0.1f));

    List<Fragmento> resultado = base.buscar(ACME, eje(0, 5, 0f), 5, UUID.randomUUID());

    assertThat(resultado).extracting(Fragmento::titulo).containsExactly("cercano", "lejano");
    assertThat(resultado).allSatisfy(f -> assertThat(f.organizacionId()).isEqualTo(ACME));
  }

  @Test
  void devuelveElFragmentoCompletoConSusIds() {
    UUID fuente = guardar(ACME, "uno", eje(0, 5, 0f));

    Fragmento encontrado = base.buscar(ACME, eje(0, 5, 0f), 1, UUID.randomUUID()).get(0);

    assertThat(encontrado.fuente()).isEqualTo(FuenteConocimiento.ARTICULO);
    assertThat(encontrado.fuenteId()).isEqualTo(fuente);
    assertThat(encontrado.titulo()).isEqualTo("uno");
    assertThat(encontrado.texto()).isEqualTo("texto uno");
  }

  @Test
  void excluyeLaFuenteIndicada() {
    UUID excluida = guardar(ACME, "excluida", eje(0, 5, 0f));
    guardar(ACME, "otra", eje(1, 5, 0f));

    List<Fragmento> resultado = base.buscar(ACME, eje(0, 5, 0f), 5, excluida);

    assertThat(resultado).extracting(Fragmento::titulo).containsExactly("otra");
  }

  @Test
  void respetaElLimiteK() {
    guardar(ACME, "a", eje(0, 5, 0f));
    guardar(ACME, "b", eje(0, 5, 0.1f));
    guardar(ACME, "c", eje(0, 5, 0.2f));

    assertThat(base.buscar(ACME, eje(0, 5, 0f), 2, UUID.randomUUID()))
        .extracting(Fragmento::titulo)
        .containsExactly("a", "b");
  }

  @Test
  void reemplazarSustituyeLosFragmentosDeLaFuente() {
    UUID fuente = UUID.randomUUID();
    base.reemplazar(
        FuenteConocimiento.ARTICULO,
        fuente,
        "h1",
        "m",
        List.of(
            indexado(ACME, fuente, "a", eje(0, 5, 0f)), indexado(ACME, fuente, "b", eje(1, 5, 0f))));

    base.reemplazar(
        FuenteConocimiento.ARTICULO,
        fuente,
        "h2",
        "m",
        List.of(indexado(ACME, fuente, "nuevo", eje(2, 5, 0f))));

    assertThat(base.buscar(ACME, eje(2, 5, 0f), 10, UUID.randomUUID()))
        .extracting(Fragmento::titulo)
        .containsExactly("nuevo");
  }

  @Test
  void estaIndexadaDistingueHashModeloYFuente() {
    UUID fuente = guardar(ACME, "x", eje(0, 5, 0f));

    assertThat(base.estaIndexada(FuenteConocimiento.ARTICULO, fuente, "hash-x", "m")).isTrue();
    assertThat(base.estaIndexada(FuenteConocimiento.ARTICULO, fuente, "otro-hash", "m")).isFalse();
    assertThat(base.estaIndexada(FuenteConocimiento.ARTICULO, fuente, "hash-x", "otro-modelo"))
        .isFalse();
    assertThat(base.estaIndexada(FuenteConocimiento.TICKET, fuente, "hash-x", "m")).isFalse();
    assertThat(base.estaIndexada(FuenteConocimiento.ARTICULO, UUID.randomUUID(), "hash-x", "m"))
        .isFalse();
  }
}
