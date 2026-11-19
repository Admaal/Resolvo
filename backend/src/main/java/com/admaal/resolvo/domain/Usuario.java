package com.admaal.resolvo.domain;

import java.time.Instant;

public final class Usuario {

  private final UsuarioId id;
  private final OrganizacionId organizacionId;
  private final String email;
  private final Rol rol;
  private final String hashContrasena;
  private final int fallosSeguidos;
  private final Instant bloqueadoHasta;

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
