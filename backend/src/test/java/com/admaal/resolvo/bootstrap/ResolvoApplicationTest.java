package com.admaal.resolvo.bootstrap;

import com.admaal.resolvo.infrastructure.PostgresDePrueba;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@Tag("full")
@SpringBootTest
class ResolvoApplicationTest {

  @DynamicPropertySource
  static void postgres(DynamicPropertyRegistry registro) {
    PostgresDePrueba.registrar(registro);
  }

  @Test
  void elContextoArranca() {}
}
