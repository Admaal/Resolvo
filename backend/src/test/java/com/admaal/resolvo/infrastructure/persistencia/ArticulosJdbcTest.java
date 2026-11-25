package com.admaal.resolvo.infrastructure.persistencia;

import static org.assertj.core.api.Assertions.assertThat;

import com.admaal.resolvo.bootstrap.ResolvoApplication;
import com.admaal.resolvo.domain.Articulo;
import com.admaal.resolvo.domain.OrganizacionId;
import com.admaal.resolvo.infrastructure.PostgresDePrueba;
import java.util.UUID;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@Tag("full")
@SpringBootTest(classes = ResolvoApplication.class)
class ArticulosJdbcTest {

  private static final OrganizacionId ACME =
      new OrganizacionId(UUID.fromString("11111111-1111-1111-1111-111111111111"));
  private static final UUID PRIMERO = UUID.fromString("66666666-6666-6666-6666-666666666661");

  @DynamicPropertySource
  static void postgres(DynamicPropertyRegistry registro) {
    PostgresDePrueba.registrar(registro);
  }

  @Autowired ArticulosJdbc articulos;
  @Autowired JdbcTemplate jdbc;

  @Test
  void listaLosArticulosDeLaOrganizacion() {
    assertThat(articulos.deLaOrganizacion(ACME)).hasSize(8).allSatisfy(a -> assertThat(a.organizacionId()).isEqualTo(ACME));
  }

  @Test
  void devuelveElArticuloCompleto() {
    Articulo articulo = articulos.porId(ACME, PRIMERO).orElseThrow();

    assertThat(articulo.titulo()).isEqualTo("Como restablecer tu contrasena");
    assertThat(articulo.cuerpo()).contains("enlace valido");
  }

  @Test
  void elArticuloDeLoginYaNoDiceQueLaCuentaSeBloquea() {
    UUID login = UUID.fromString("66666666-6666-6666-6666-666666666662");

    Articulo articulo = articulos.porId(ACME, login).orElseThrow();

    assertThat(articulo.cuerpo())
        .doesNotContain("se bloquea unos minutos")
        .contains("la cuenta no se bloquea")
        .contains("15 minutos");
  }

  @Test
  void otraOrganizacionNoVeNada() {
    UUID otra = UUID.randomUUID();
    jdbc.update("insert into organizacion (id, nombre) values (?, 'Otra')", otra);
    OrganizacionId ajena = new OrganizacionId(otra);

    assertThat(articulos.deLaOrganizacion(ajena)).isEmpty();
    assertThat(articulos.porId(ajena, PRIMERO)).isEmpty();
    assertThat(articulos.porId(ACME, UUID.randomUUID())).isEmpty();
  }
}
