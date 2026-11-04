package com.admaal.resolvo.domain;

import java.time.Duration;
import java.time.Instant;

public final class Usuario {

  private final UsuarioId id;
  private final OrganizacionId organizacionId;
  private final String email;
  private final Rol rol;
  private final String hashContrasena;
  private int fallosSeguidos;
  private Instant bloqueadoHasta;

  public Usuario(
      UsuarioId id,
      OrganizacionId organizacionId,
      String email,
      Rol rol,
      String hashContrasena,
      int fallosSeguidos,
      Instant bloqueadoHasta) {
    this.id = id;
    this.organizacionId = organizacionId;
    this.email = email;
    this.rol = rol;
    this.hashContrasena = hashContrasena;
    this.fallosSeguidos = fallosSeguidos;
    this.bloqueadoHasta = bloqueadoHasta;
  }

  public static Usuario nuevo(
      UsuarioId id, OrganizacionId organizacionId, String email, Rol rol, String hashContrasena) {
    return new Usuario(id, organizacionId, email, rol, hashContrasena, 0, null);
  }

  public boolean estaBloqueado(Instant ahora) {
    return bloqueadoHasta != null && ahora.isBefore(bloqueadoHasta);
  }

  public void registrarFallo(Instant ahora) {
    fallosSeguidos = fallosSeguidos + 1;
    if (fallosSeguidos >= 5) {
      bloqueadoHasta = ahora.plus(Duration.ofMinutes(15));
    }
  }

  public void reiniciarFallos() {
    fallosSeguidos = 0;
    bloqueadoHasta = null;
  }

  public UsuarioId id() {
    return id;
  }

  public OrganizacionId organizacionId() {
    return organizacionId;
  }

  public String email() {
    return email;
  }

  public Rol rol() {
    return rol;
  }

  public String hashContrasena() {
    return hashContrasena;
  }

  public int fallosSeguidos() {
    return fallosSeguidos;
  }

  public Instant bloqueadoHasta() {
    return bloqueadoHasta;
  }

  public Usuario copia() {
    return new Usuario(
        id(), organizacionId(), email(), rol(), hashContrasena(), fallosSeguidos(), bloqueadoHasta());
  }
}
