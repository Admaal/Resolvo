package com.admaal.resolvo.infrastructure.web;

import com.admaal.resolvo.application.Reloj;
import com.admaal.resolvo.domain.LimiteDeFrecuencia;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Duration;
import java.util.Locale;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Limita los intentos de login por (email, cliente) y se reinicia con un login correcto. Cuenta el
 * intento antes de comprobar la contrasena, de modo que una rafaga en paralelo no pasa de N. Sustituye
 * al bloqueo de la cuenta: con cuentas de demo publicas, bloquear la cuenta dejaba a cualquiera sin
 * entrar. Solo funciona como se espera si la clave distingue de verdad a los clientes (ver clave()).
 */
@Component
public class LimiteDeLogin {

  /** Tope del email dentro de la clave: un email real no pasa de 254 y asi la memoria no crece a voluntad. */
  static final int MAXIMO_DEL_EMAIL = 254;

  private final LimiteDeFrecuencia limite;
  private final Reloj reloj;

  public LimiteDeLogin(
      Reloj reloj,
      @Value("${resolvo.limites.login-fallos:5}") int fallos,
      @Value("${resolvo.limites.login-ventana-minutos:15}") int minutos) {
    this.reloj = reloj;
    this.limite = new LimiteDeFrecuencia(fallos, Duration.ofMinutes(minutos));
  }

  /**
   * Clave del intento. El cliente es CF-Connecting-IP (Cloudflare) si viene y, si no, la direccion
   * remota. Es falsificable llamando directamente a la API: vale para que nadie bloquee a otro,
   * no como defensa contra un atacante decidido.
   */
  String clave(String email, HttpServletRequest peticion) {
    String normalizado = email == null ? "" : email.strip().toLowerCase(Locale.ROOT);
    if (normalizado.length() > MAXIMO_DEL_EMAIL) {
      normalizado = normalizado.substring(0, MAXIMO_DEL_EMAIL);
    }
    String cabecera = peticion.getHeader("CF-Connecting-IP");
    String cliente = cabecera == null || cabecera.isBlank() ? peticion.getRemoteAddr() : cabecera.strip();
    return normalizado + "|" + cliente;
  }

  /** Cuenta un intento y lanza {@link DemasiadasPeticiones} si ya no caben mas. */
  void intentar(String clave) {
    var decision = limite.admitir(clave, reloj.ahora());
    if (!decision.admitida()) {
      throw new DemasiadasPeticiones("demasiados-intentos", decision.reintentarEn());
    }
  }

  void correcto(String clave) {
    limite.reiniciar(clave);
  }
}
