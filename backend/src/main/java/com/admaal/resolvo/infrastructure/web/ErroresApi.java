package com.admaal.resolvo.infrastructure.web;

import com.admaal.resolvo.domain.CredencialesInvalidas;
import com.admaal.resolvo.domain.CuentaBloqueada;
import com.admaal.resolvo.domain.NoEncontrado;
import com.admaal.resolvo.domain.Prohibido;
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

  @ExceptionHandler({HttpMessageNotReadableException.class, MethodArgumentTypeMismatchException.class})
  ResponseEntity<Map<String, String>> ilegible() {
    return ResponseEntity.unprocessableEntity().body(Map.of("error", "invalido"));
  }

  @ExceptionHandler(CredencialesInvalidas.class)
  ResponseEntity<Map<String, String>> noAutorizado(CredencialesInvalidas error) {
    return ResponseEntity.status(401).body(Map.of("error", error.getMessage()));
  }

  @ExceptionHandler(CuentaBloqueada.class)
  ResponseEntity<Map<String, String>> bloqueado(CuentaBloqueada error) {
    return ResponseEntity.status(401).body(Map.of("error", error.getMessage()));
  }
}
