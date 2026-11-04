package com.admaal.resolvo.infrastructure.persistencia;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "categoria")
public class CategoriaEntity {

  @Id
  private UUID id;

  @Column(nullable = false)
  private UUID organizacionId;

  @Column(nullable = false, length = 80)
  private String nombre;

  public UUID getId() {
    return id;
  }

  public UUID getOrganizacionId() {
    return organizacionId;
  }

  public String getNombre() {
    return nombre;
  }
}
