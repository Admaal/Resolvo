package com.admaal.resolvo.infrastructure.persistencia;

import com.admaal.resolvo.domain.Rol;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "usuario")
public class UsuarioEntity {

  @Id
  private UUID id;

  @Column(nullable = false)
  private UUID organizacionId;

  @Column(nullable = false, length = 320)
  private String email;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private Rol rol;

  @Column(nullable = false, length = 100)
  private String hashContrasena;

  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public UUID getOrganizacionId() {
    return organizacionId;
  }

  public void setOrganizacionId(UUID organizacionId) {
    this.organizacionId = organizacionId;
  }

  public String getEmail() {
    return email;
  }

  public void setEmail(String email) {
    this.email = email;
  }

  public Rol getRol() {
    return rol;
  }

  public void setRol(Rol rol) {
    this.rol = rol;
  }

  public String getHashContrasena() {
    return hashContrasena;
  }

  public void setHashContrasena(String hashContrasena) {
    this.hashContrasena = hashContrasena;
  }
}
