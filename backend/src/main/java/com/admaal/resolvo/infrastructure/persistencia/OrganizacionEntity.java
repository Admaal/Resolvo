package com.admaal.resolvo.infrastructure.persistencia;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "organizacion")
public class OrganizacionEntity {

  @Id
  private UUID id;

  @Column(nullable = false, length = 120)
  private String nombre;

  public UUID getId() {
    return id;
  }

  public String getNombre() {
    return nombre;
  }
}
