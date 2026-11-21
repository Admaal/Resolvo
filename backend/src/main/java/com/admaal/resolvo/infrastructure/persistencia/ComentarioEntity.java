package com.admaal.resolvo.infrastructure.persistencia;

import com.admaal.resolvo.domain.MotivoModeracion;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
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

  @Enumerated(EnumType.STRING)
  @Column(length = 20)
  private MotivoModeracion ocultoMotivo;

  private UUID ocultoPor;

  private Instant ocultoEn;

  @Column(nullable = false)
  private boolean semilla;

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

  public MotivoModeracion getOcultoMotivo() {
    return ocultoMotivo;
  }

  public void setOcultoMotivo(MotivoModeracion ocultoMotivo) {
    this.ocultoMotivo = ocultoMotivo;
  }

  public UUID getOcultoPor() {
    return ocultoPor;
  }

  public void setOcultoPor(UUID ocultoPor) {
    this.ocultoPor = ocultoPor;
  }

  public Instant getOcultoEn() {
    return ocultoEn;
  }

  public void setOcultoEn(Instant ocultoEn) {
    this.ocultoEn = ocultoEn;
  }

  public boolean isSemilla() {
    return semilla;
  }

  public void setSemilla(boolean semilla) {
    this.semilla = semilla;
  }
}
