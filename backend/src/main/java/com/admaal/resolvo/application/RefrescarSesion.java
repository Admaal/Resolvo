package com.admaal.resolvo.application;

import com.admaal.resolvo.domain.BonoRefresh;
import com.admaal.resolvo.domain.CredencialesInvalidas;
import com.admaal.resolvo.domain.Usuario;
import java.time.Duration;

public final class RefrescarSesion {

  private final Reloj reloj;
  private final Usuarios usuarios;
  private final EmisorDeTokens tokens;
  private final SesionesRefresh sesiones;

  public RefrescarSesion(
      Reloj reloj, Usuarios usuarios, EmisorDeTokens tokens, SesionesRefresh sesiones) {
    this.reloj = reloj;
    this.usuarios = usuarios;
    this.tokens = tokens;
    this.sesiones = sesiones;
  }

  public SesionIniciada ejecutar(String token) {
    BonoRefresh bono = sesiones.porToken(token).orElseThrow(CredencialesInvalidas::new);
    if (bono.inservible()) {
      sesiones.revocarFamilia(bono.familia());
      throw new CredencialesInvalidas();
    }
    bono.consumir();
    sesiones.guardar(bono);
    Usuario usuario = usuarios.porId(bono.usuarioId()).orElseThrow(CredencialesInvalidas::new);
    String refresh = sesiones.tokenNuevo();
    sesiones.guardar(bono.rotado(refresh));
    String access = tokens.emitir(usuario, reloj.ahora().plus(Duration.ofMinutes(15)));
    return new SesionIniciada(access, refresh);
  }
}
