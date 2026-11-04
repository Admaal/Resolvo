package com.admaal.resolvo.application;

import com.admaal.resolvo.domain.BonoRefresh;
import com.admaal.resolvo.domain.CredencialesInvalidas;
import com.admaal.resolvo.domain.CuentaBloqueada;
import com.admaal.resolvo.domain.Usuario;
import java.time.Duration;
import java.time.Instant;

public final class IniciarSesion {

  private final Reloj reloj;
  private final Usuarios usuarios;
  private final VerificadorContrasena verificador;
  private final EmisorDeTokens tokens;
  private final SesionesRefresh sesiones;

  public IniciarSesion(
      Reloj reloj,
      Usuarios usuarios,
      VerificadorContrasena verificador,
      EmisorDeTokens tokens,
      SesionesRefresh sesiones) {
    this.reloj = reloj;
    this.usuarios = usuarios;
    this.verificador = verificador;
    this.tokens = tokens;
    this.sesiones = sesiones;
  }

  public SesionIniciada ejecutar(String email, String contrasena) {
    Usuario usuario = usuarios.porEmail(email).orElseThrow(CredencialesInvalidas::new);
    Instant ahora = reloj.ahora();
    if (usuario.estaBloqueado(ahora)) {
      throw new CuentaBloqueada();
    }
    if (!verificador.coincide(contrasena, usuario.hashContrasena())) {
      usuario.registrarFallo(ahora);
      usuarios.guardar(usuario);
      if (usuario.estaBloqueado(reloj.ahora())) {
        throw new CuentaBloqueada();
      }
      throw new CredencialesInvalidas();
    }
    usuario.reiniciarFallos();
    usuarios.guardar(usuario);
    String refresh = sesiones.tokenNuevo();
    sesiones.guardar(BonoRefresh.nuevo(refresh, usuario.id()));
    String access = tokens.emitir(usuario, ahora.plus(Duration.ofMinutes(15)));
    return new SesionIniciada(access, refresh);
  }
}
