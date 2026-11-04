package com.admaal.resolvo.infrastructure.persistencia;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;

@Entity
@Table(name = "bono_refresh")
public class BonoEntity {

  @Id
  @Column(length = 64)
  private String tokenHash;

  @Column(nullable = false)
  private UUID usuarioId;

  @Column(nullable = false)
  private UUID familia;

  @Column(nullable = false)
  private boolean consumido;

  @Column(nullable = false)
  private boolean revocado;

  public String getTokenHash() {
    return tokenHash;
  }

  public void setTokenHash(String tokenHash) {
    this.tokenHash = tokenHash;
  }

  public UUID getUsuarioId() {
    return usuarioId;
  }

  public void setUsuarioId(UUID usuarioId) {
    this.usuarioId = usuarioId;
  }

  public UUID getFamilia() {
    return familia;
  }

  public void setFamilia(UUID familia) {
    this.familia = familia;
  }

  public boolean isConsumido() {
    return consumido;
  }

  public void setConsumido(boolean consumido) {
    this.consumido = consumido;
  }

  public boolean isRevocado() {
    return revocado;
  }

  public void setRevocado(boolean revocado) {
    this.revocado = revocado;
  }
}
