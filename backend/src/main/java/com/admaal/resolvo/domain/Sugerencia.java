package com.admaal.resolvo.domain;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public final class Sugerencia {

  private final SugerenciaId id;
  private final OrganizacionId organizacionId;
  private final TicketId ticketId;
  private final String borrador;
  private final String modelo;
  private final Instant creadaEn;
  private final List<Cita> citas;
  private EstadoSugerencia estado;
  private UsuarioId resueltaPor;
  private Instant resueltaEn;

  private Sugerencia(
      SugerenciaId id,
      OrganizacionId organizacionId,
      TicketId ticketId,
      String borrador,
      String modelo,
      Instant creadaEn,
      List<Cita> citas) {
    this.id = id;
    this.organizacionId = organizacionId;
    this.ticketId = ticketId;
    this.borrador = TextoPlano.exigir(borrador, 8000);
    this.modelo = modelo;
    this.creadaEn = creadaEn;
    this.citas = List.copyOf(citas);
    this.estado = EstadoSugerencia.PENDIENTE;
  }

  public static Sugerencia crear(
      SugerenciaId id,
      OrganizacionId organizacionId,
      TicketId ticketId,
      String borrador,
      String modelo,
      Instant creadaEn,
      List<FragmentoId> citados,
      Set<FragmentoId> recuperados) {
    List<Cita> citas = new ArrayList<>();
    for (FragmentoId fragmento : new LinkedHashSet<>(citados)) {
      if (recuperados.contains(fragmento)) {
        citas.add(new Cita(fragmento, citas.size() + 1));
      }
    }
    return new Sugerencia(id, organizacionId, ticketId, borrador, modelo, creadaEn, citas);
  }

  public static Sugerencia rehidratar(
      SugerenciaId id,
      OrganizacionId organizacionId,
      TicketId ticketId,
      String borrador,
      String modelo,
      Instant creadaEn,
      List<Cita> citas,
      EstadoSugerencia estado,
      UsuarioId resueltaPor,
      Instant resueltaEn) {
    Sugerencia sugerencia =
        new Sugerencia(id, organizacionId, ticketId, borrador, modelo, creadaEn, citas);
    sugerencia.estado = estado;
    sugerencia.resueltaPor = resueltaPor;
    sugerencia.resueltaEn = resueltaEn;
    return sugerencia;
  }

  public String aprobar(UsuarioId actor, Rol rol, String textoFinal, Instant ahora) {
    exigirResoluble(rol);
    String texto = TextoPlano.exigir(textoFinal, 8000);
    resolver(EstadoSugerencia.APROBADA, actor, ahora);
    return texto;
  }

  public void descartar(UsuarioId actor, Rol rol, Instant ahora) {
    exigirResoluble(rol);
    resolver(EstadoSugerencia.DESCARTADA, actor, ahora);
  }

  private void exigirResoluble(Rol rol) {
    if (rol == Rol.CLIENTE) {
      throw new Prohibido();
    }
    if (estado != EstadoSugerencia.PENDIENTE) {
      throw new TransicionNoPermitida();
    }
  }

  private void resolver(EstadoSugerencia destino, UsuarioId actor, Instant ahora) {
    estado = destino;
    resueltaPor = actor;
    resueltaEn = ahora;
  }

  public SugerenciaId id() {
    return id;
  }

  public OrganizacionId organizacionId() {
    return organizacionId;
  }

  public TicketId ticketId() {
    return ticketId;
  }

  public String borrador() {
    return borrador;
  }

  public String modelo() {
    return modelo;
  }

  public Instant creadaEn() {
    return creadaEn;
  }

  public List<Cita> citas() {
    return citas;
  }

  public EstadoSugerencia estado() {
    return estado;
  }

  public UsuarioId resueltaPor() {
    return resueltaPor;
  }

  public Instant resueltaEn() {
    return resueltaEn;
  }
}
