package com.admaal.resolvo.infrastructure.persistencia;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "comentario")
public class ComentarioEntity {

  @Id
  private UUID id;

  @Column(nullable = false)
  private UUID autorId;

  @Column(nullable = false, length = 8000)
  private String cuerpo;

  @Column(nullable = false)
  private boolean interno;

  @Column(nullable = false)
  private Instant creadoEn;

  public UUID getId() {
    return id;
  }

  public void setId(UUID id) {
    this.id = id;
  }

  public UUID getAutorId() {
    return autorId;
  }

  public void setAutorId(UUID autorId) {
    this.autorId = autorId;
  }

  public String getCuerpo() {
    return cuerpo;
  }

  public void setCuerpo(String cuerpo) {
    this.cuerpo = cuerpo;
  }

  public boolean isInterno() {
    return interno;
  }

  public void setInterno(boolean interno) {
    this.interno = interno;
  }

  public Instant getCreadoEn() {
    return creadoEn;
  }

  public void setCreadoEn(Instant creadoEn) {
    this.creadoEn = creadoEn;
  }
}
