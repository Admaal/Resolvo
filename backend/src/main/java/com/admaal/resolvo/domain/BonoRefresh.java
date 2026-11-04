package com.admaal.resolvo.domain;

import java.util.UUID;

public final class BonoRefresh {

  private final String token;
  private final UsuarioId usuarioId;
  private final UUID familia;
  private boolean consumido;
  private boolean revocado;

  public BonoRefresh(String token, UsuarioId usuarioId, UUID familia, boolean consumido, boolean revocado) {
    this.token = token;
    this.usuarioId = usuarioId;
    this.familia = familia;
    this.consumido = consumido;
    this.revocado = revocado;
  }

  public static BonoRefresh nuevo(String token, UsuarioId usuarioId) {
    return new BonoRefresh(token, usuarioId, UUID.randomUUID(), false, false);
  }

  public BonoRefresh rotado(String tokenNuevo) {
    return new BonoRefresh(tokenNuevo, usuarioId, familia, false, false);
  }

  public void consumir() {
    consumido = true;
  }

  public void revocar() {
    revocado = true;
  }

  public boolean inservible() {
    return consumido || revocado;
  }

  public String token() {
    return token;
  }

  public UsuarioId usuarioId() {
    return usuarioId;
  }

  public UUID familia() {
    return familia;
  }

  public boolean consumido() {
    return consumido;
  }

  public boolean revocado() {
    return revocado;
  }
}
