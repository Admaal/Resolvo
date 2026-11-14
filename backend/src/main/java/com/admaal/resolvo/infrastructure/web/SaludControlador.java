package com.admaal.resolvo.infrastructure.web;

import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Health check público para Render y para el ping que evita que la API se duerma. */
@RestController
@RequestMapping("/api/salud")
public class SaludControlador {

  @GetMapping
  Map<String, String> salud() {
    return Map.of("estado", "ok");
  }
}
