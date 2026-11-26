package com.admaal.resolvo.infrastructure.persistencia;

import static org.assertj.core.api.Assertions.assertThat;

import com.admaal.resolvo.bootstrap.ResolvoApplication;
import com.admaal.resolvo.infrastructure.PostgresDePrueba;
import java.util.List;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@Tag("full")
@SpringBootTest(classes = ResolvoApplication.class)
class MigracionesTest {

  @DynamicPropertySource
  static void postgres(DynamicPropertyRegistry registro) {
    PostgresDePrueba.registrar(registro);
  }

  @Autowired JdbcTemplate jdbc;

  @Test
  void laV10QuitaLasColumnasDelBloqueoDeCuenta() {
    List<String> columnas =
        jdbc.queryForList(
            "select column_name from information_schema.columns where table_name = 'usuario'",
            String.class);

    assertThat(columnas).contains("email", "hash_contrasena");
    assertThat(columnas).doesNotContain("fallos_seguidos", "bloqueado_hasta");
  }
}
