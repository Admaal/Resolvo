package com.admaal.resolvo.infrastructure.web;

import com.admaal.resolvo.application.IaNoDisponible;
import com.admaal.resolvo.application.PresupuestoAgotado;
import com.admaal.resolvo.domain.CredencialesInvalidas;
import com.admaal.resolvo.domain.DemasiadosEnlaces;
import com.admaal.resolvo.domain.NoEncontrado;
import com.admaal.resolvo.domain.Prohibido;
import com.admaal.resolvo.domain.SinFuentes;
import com.admaal.resolvo.domain.TextoInvalido;
import com.admaal.resolvo.domain.TransicionNoPermitida;
import java.util.Map;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

@RestControllerAdvice
public class ErroresApi {

  @ExceptionHandler(NoEncontrado.class)
  ResponseEntity<Map<String, String>> noEncontrado(NoEncontrado error) {
    return ResponseEntity.status(404).body(Map.of("error", error.getMessage()));
  }

  @ExceptionHandler(TransicionNoPermitida.class)
  ResponseEntity<Map<String, String>> conflicto(TransicionNoPermitida error) {
    return ResponseEntity.status(409).body(Map.of("error", error.getMessage()));
  }

  @ExceptionHandler(Prohibido.class)
  ResponseEntity<Map<String, String>> prohibido(Prohibido error) {
    return ResponseEntity.status(403).body(Map.of("error", error.getMessage()));
  }

  @ExceptionHandler(TextoInvalido.class)
  ResponseEntity<Map<String, String>> invalido(TextoInvalido error) {
    return ResponseEntity.unprocessableEntity().body(Map.of("error", error.getMessage()));
  }

  @ExceptionHandler(DemasiadosEnlaces.class)
  ResponseEntity<Map<String, String>> demasiadosEnlaces(DemasiadosEnlaces error) {
    return ResponseEntity.unprocessableEntity().body(Map.of("error", error.getMessage()));
  }

  @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
  ResponseEntity<Map<String, String>> ilegible() {
    return ResponseEntity.unprocessableEntity().body(Map.of("error", "invalido"));
  }

  @ExceptionHandler(CredencialesInvalidas.class)
  ResponseEntity<Map<String, String>> noAutorizado(CredencialesInvalidas error) {
    return ResponseEntity.status(401).body(Map.of("error", error.getMessage()));
  }

  @ExceptionHandler(DemasiadasPeticiones.class)
  ResponseEntity<Map<String, String>> demasiadasPeticiones(DemasiadasPeticiones error) {
    return ResponseEntity.status(429)
        .header("Retry-After", Long.toString(error.segundosParaReintentar()))
        .body(Map.of("error", error.getMessage()));
  }

  @ExceptionHandler(PresupuestoAgotado.class)
  ResponseEntity<Map<String, String>> presupuestoAgotado(PresupuestoAgotado error) {
    return ResponseEntity.status(429).body(Map.of("error", error.getMessage()));
  }

  @ExceptionHandler(SinFuentes.class)
  ResponseEntity<Map<String, String>> sinFuentes() {
    return ResponseEntity.unprocessableEntity().body(Map.of("error", "sin-fuentes"));
  }

  @ExceptionHandler(IaNoDisponible.class)
  ResponseEntity<Map<String, String>> iaNoDisponible(IaNoDisponible error) {
    return ResponseEntity.status(503).body(Map.of("error", error.getMessage()));
  }
}
