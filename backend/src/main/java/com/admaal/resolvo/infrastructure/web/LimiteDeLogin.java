package com.admaal.resolvo.infrastructure.web;

import com.admaal.resolvo.application.Reloj;
import com.admaal.resolvo.domain.Limite;
import com.admaal.resolvo.infrastructure.FabricaDeLimites;
import jakarta.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
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

  /** Tope de la IP que manda el proxy: una IPv6 completa mide 45. */
  static final int MAXIMO_DE_LA_IP = 64;

  private final Limite limite;
  private final Reloj reloj;
  private final byte[] secretoDelProxy;

  public LimiteDeLogin(
      Reloj reloj,
      FabricaDeLimites limites,
      @Value("${resolvo.limites.login-fallos:5}") int fallos,
      @Value("${resolvo.limites.login-ventana-minutos:15}") int minutos,
      @Value("${resolvo.proxy.clave-compartida:}") String secretoDelProxy) {
    this.reloj = reloj;
    this.secretoDelProxy = secretoDelProxy.getBytes(StandardCharsets.UTF_8);
    this.limite = limites.crear("login", fallos, Duration.ofMinutes(minutos));
  }

  /**
   * Clave del intento: email e IP del cliente. La IP sale, por orden, de X-Cliente-Ip si llega con el
   * secreto del proxy de Pages (entre zonas de Cloudflare, CF-Connecting-IP llega como la IP del
   * Worker, la misma para todos), de CF-Connecting-IP (Cloudflare la impone y no se puede forjar) o
   * de la direccion remota. Sin secreto configurado, X-Cliente-Ip nunca se acepta.
   */
  String clave(String email, HttpServletRequest peticion) {
    String normalizado = email == null ? "" : email.strip().toLowerCase(Locale.ROOT);
    if (normalizado.length() > MAXIMO_DEL_EMAIL) {
      normalizado = normalizado.substring(0, MAXIMO_DEL_EMAIL);
    }
    return normalizado + "|" + ipDelCliente(peticion);
  }

  private String ipDelCliente(HttpServletRequest peticion) {
    String firmada = ipFirmadaPorElProxy(peticion);
    if (firmada != null) {
      return firmada;
    }
    String cabecera = peticion.getHeader("CF-Connecting-IP");
    return cabecera == null || cabecera.isBlank() ? peticion.getRemoteAddr() : cabecera.strip();
  }

  private String ipFirmadaPorElProxy(HttpServletRequest peticion) {
    String presentado = peticion.getHeader("X-Resolvo-Proxy");
    String ip = peticion.getHeader("X-Cliente-Ip");
    if (secretoDelProxy.length == 0 || presentado == null || ip == null || ip.isBlank()) {
      return null;
    }
    boolean coincide =
        MessageDigest.isEqual(presentado.getBytes(StandardCharsets.UTF_8), secretoDelProxy);
    if (!coincide) {
      return null;
    }
    String limpia = ip.strip();
    return limpia.length() > MAXIMO_DE_LA_IP ? limpia.substring(0, MAXIMO_DE_LA_IP) : limpia;
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
