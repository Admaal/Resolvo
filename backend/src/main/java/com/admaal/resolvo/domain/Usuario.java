package com.admaal.resolvo.domain;

public final class Usuario {

  private final UsuarioId id;
  private final OrganizacionId organizacionId;
  private final String email;
  private final Rol rol;
  private final String hashContrasena;

  public Usuario(
      UsuarioId id,
      OrganizacionId organizacionId,
      String email,
      Rol rol,
      String hashContrasena) {
    this.id = id;
    this.organizacionId = organizacionId;
    this.email = email;
    this.rol = rol;
    this.hashContrasena = hashContrasena;
  }

  public static Usuario nuevo(
      UsuarioId id, OrganizacionId organizacionId, String email, Rol rol, String hashContrasena) {
    return new Usuario(id, organizacionId, email, rol, hashContrasena);
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

  public Usuario copia() {
    return new Usuario(
        id(), organizacionId(), email(), rol(), hashContrasena());
  }
}
