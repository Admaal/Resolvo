package com.admaal.resolvo.domain;

import java.time.Instant;

/** Propuesta de categoria y prioridad para un ticket; una persona la aplica, la corrige o la descarta. */
public final class Clasificacion {

  private final ClasificacionId id;
  private final OrganizacionId organizacionId;
  private final TicketId ticketId;
  private final CategoriaId categoriaId;
  private final Prioridad prioridad;
  private final String motivo;
  private final String modelo;
  private final Instant creadaEn;
  private EstadoClasificacion estado;
  private UsuarioId resueltaPor;
  private Instant resueltaEn;

  private Clasificacion(
      ClasificacionId id,
      OrganizacionId organizacionId,
      TicketId ticketId,
      CategoriaId categoriaId,
      Prioridad prioridad,
      String motivo,
      String modelo,
      Instant creadaEn) {
    this.id = id;
    this.organizacionId = organizacionId;
    this.ticketId = ticketId;
    this.categoriaId = categoriaId;
    this.prioridad = prioridad;
    this.motivo = TextoPlano.exigir(motivo, 500);
    this.modelo = modelo;
    this.creadaEn = creadaEn;
    this.estado = EstadoClasificacion.PENDIENTE;
  }

  public static Clasificacion proponer(
      ClasificacionId id,
      OrganizacionId organizacionId,
      TicketId ticketId,
      CategoriaId categoriaId,
      Prioridad prioridad,
      String motivo,
      String modelo,
      Instant creadaEn) {
    return new Clasificacion(
        id, organizacionId, ticketId, categoriaId, prioridad, motivo, modelo, creadaEn);
  }

  public static Clasificacion rehidratar(
      ClasificacionId id,
      OrganizacionId organizacionId,
      TicketId ticketId,
      CategoriaId categoriaId,
      Prioridad prioridad,
      String motivo,
      String modelo,
      Instant creadaEn,
      EstadoClasificacion estado,
      UsuarioId resueltaPor,
      Instant resueltaEn) {
    Clasificacion clasificacion =
        new Clasificacion(
            id, organizacionId, ticketId, categoriaId, prioridad, motivo, modelo, creadaEn);
    clasificacion.estado = estado;
    clasificacion.resueltaPor = resueltaPor;
    clasificacion.resueltaEn = resueltaEn;
    return clasificacion;
  }

  public void aplicar(UsuarioId actor, Rol rol, Instant ahora) {
    exigirResoluble(rol);
    resolver(EstadoClasificacion.APLICADA, actor, ahora);
  }

  public void descartar(UsuarioId actor, Rol rol, Instant ahora) {
    exigirResoluble(rol);
    resolver(EstadoClasificacion.DESCARTADA, actor, ahora);
  }

  private void exigirResoluble(Rol rol) {
    if (rol == Rol.CLIENTE) {
      throw new Prohibido();
    }
    if (estado != EstadoClasificacion.PENDIENTE) {
      throw new TransicionNoPermitida();
    }
  }

  private void resolver(EstadoClasificacion destino, UsuarioId actor, Instant ahora) {
    estado = destino;
    resueltaPor = actor;
    resueltaEn = ahora;
  }

  public ClasificacionId id() {
    return id;
  }

  public OrganizacionId organizacionId() {
    return organizacionId;
  }

  public TicketId ticketId() {
    return ticketId;
  }

  public CategoriaId categoriaId() {
    return categoriaId;
  }

  public Prioridad prioridad() {
    return prioridad;
  }

  public String motivo() {
    return motivo;
  }

  public String modelo() {
    return modelo;
  }

  public Instant creadaEn() {
    return creadaEn;
  }

  public EstadoClasificacion estado() {
    return estado;
  }

  public UsuarioId resueltaPor() {
    return resueltaPor;
  }

  public Instant resueltaEn() {
    return resueltaEn;
  }
}
