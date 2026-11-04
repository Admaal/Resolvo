package com.admaal.resolvo.infrastructure.web;

import com.admaal.resolvo.infrastructure.AplicacionDeResolvo;
import com.admaal.resolvo.infrastructure.seguridad.SeguridadConfig.CookiePropiedades;
import java.time.Duration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/sesion")
public class SesionControlador {

  static final String COOKIE = "refresh";

  private final AplicacionDeResolvo aplicacion;
  private final CookiePropiedades cookies;

  public SesionControlador(AplicacionDeResolvo aplicacion, CookiePropiedades cookies) {
    this.aplicacion = aplicacion;
    this.cookies = cookies;
  }

  @PostMapping
  ResponseEntity<Access> entrar(@RequestBody Credenciales cuerpo) {
    var sesion = aplicacion.entrar(cuerpo.email(), cuerpo.contrasena());
    return ResponseEntity.ok().header(HttpHeaders.SET_COOKIE, cookie(sesion.refresh()).toString()).body(new Access(sesion.access()));
  }

  @PostMapping("/refresh")
  ResponseEntity<Access> refrescar(@CookieValue(name = COOKIE, required = false) String refresh) {
    if (refresh == null || refresh.isBlank()) {
      return ResponseEntity.status(401).body(null);
    }
    var sesion = aplicacion.refrescar(refresh);
    return ResponseEntity.ok().header(HttpHeaders.SET_COOKIE, cookie(sesion.refresh()).toString()).body(new Access(sesion.access()));
  }

  @DeleteMapping
  ResponseEntity<Void> cerrar(@CookieValue(name = COOKIE, required = false) String refresh) {
    if (refresh != null && !refresh.isBlank()) {
      aplicacion.cerrar(refresh);
    }
    return ResponseEntity.noContent().header(HttpHeaders.SET_COOKIE, cookie("").toString()).build();
  }

  private ResponseCookie cookie(String valor) {
    boolean borrar = valor == null || valor.isEmpty();
    return ResponseCookie.from(COOKIE, borrar ? "" : valor)
        .httpOnly(true)
        .secure(cookies.secure())
        .sameSite("Lax")
        .path("/api/sesion")
        .maxAge(borrar ? Duration.ZERO : Duration.ofDays(14))
        .build();
  }

  public record Credenciales(String email, String contrasena) {}

  public record Access(String access) {}
}
