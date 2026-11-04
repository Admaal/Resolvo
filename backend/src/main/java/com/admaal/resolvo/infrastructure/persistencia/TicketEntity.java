package com.admaal.resolvo.infrastructure.persistencia;

import com.admaal.resolvo.domain.EstadoTicket;
import com.admaal.resolvo.domain.Prioridad;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "ticket")
public class TicketEntity {

  @Id
  private UUID id;

  @Column(nullable = false)
  private UUID organizacionId;

  @Column(nullable = false)
  private UUID autorId;

  @Column(nullable = false)
  private UUID categoriaId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private Prioridad prioridad;

  @Column(nullable = false, length = 40)
  private String codigo;

  @Column(nullable = false, length = 200)
  private String asunto;

  @Column(nullable = false, length = 8000)
  private String cuerpo;

  @Column(nullable = false)
  private Instant abiertoEn;

  @Column(nullable = false)
  private Instant venceEl;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 20)
  private EstadoTicket estado;

  private UUID asignadoId;

  @OneToMany(cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.EAGER)
  @JoinColumn(name = "ticket_id", nullable = false)
  private List<ComentarioEntity> comentarios = new ArrayList<>();

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

  public UUID getAutorId() {
    return autorId;
  }

  public void setAutorId(UUID autorId) {
    this.autorId = autorId;
  }

  public UUID getCategoriaId() {
    return categoriaId;
  }

  public void setCategoriaId(UUID categoriaId) {
    this.categoriaId = categoriaId;
  }

  public Prioridad getPrioridad() {
    return prioridad;
  }

  public void setPrioridad(Prioridad prioridad) {
    this.prioridad = prioridad;
  }

  public String getCodigo() {
    return codigo;
  }

  public void setCodigo(String codigo) {
    this.codigo = codigo;
  }

  public String getAsunto() {
    return asunto;
  }

  public void setAsunto(String asunto) {
    this.asunto = asunto;
  }

  public String getCuerpo() {
    return cuerpo;
  }

  public void setCuerpo(String cuerpo) {
    this.cuerpo = cuerpo;
  }

  public Instant getAbiertoEn() {
    return abiertoEn;
  }

  public void setAbiertoEn(Instant abiertoEn) {
    this.abiertoEn = abiertoEn;
  }

  public Instant getVenceEl() {
    return venceEl;
  }

  public void setVenceEl(Instant venceEl) {
    this.venceEl = venceEl;
  }

  public EstadoTicket getEstado() {
    return estado;
  }

  public void setEstado(EstadoTicket estado) {
    this.estado = estado;
  }

  public UUID getAsignadoId() {
    return asignadoId;
  }

  public void setAsignadoId(UUID asignadoId) {
    this.asignadoId = asignadoId;
  }

  public List<ComentarioEntity> getComentarios() {
    return comentarios;
  }
}
