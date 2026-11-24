package com.admaal.resolvo.domain;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public final class Ticket {

  /** Un cliente puede pegar hasta este numero de enlaces por comentario (frena el spam). */
  private static final int MAXIMO_ENLACES_DE_CLIENTE = 3;

  private static final Pattern ENLACE = Pattern.compile("(?i)(?:https?://(?:www\\.)?|www\\.)");

  private final TicketId id;
  private final OrganizacionId organizacionId;
  private final UsuarioId autorId;
  private CategoriaId categoriaId;
  private Prioridad prioridad;
  private final String codigo;
  private final String asunto;
  private final String cuerpo;
  private final Instant abiertoEn;
  private Instant venceEl;
  private EstadoTicket estado;
  private UsuarioId asignadoId;
  private final List<Comentario> comentarios = new ArrayList<>();

  private Ticket(
      TicketId id,
      OrganizacionId organizacionId,
      UsuarioId autorId,
      CategoriaId categoriaId,
      Prioridad prioridad,
      String codigo,
      String asunto,
      String cuerpo,
      Instant abiertoEn,
      Instant venceEl) {
    this.id = id;
    this.organizacionId = organizacionId;
    this.autorId = autorId;
    this.categoriaId = categoriaId;
    this.prioridad = prioridad;
    this.codigo = codigo;
    this.asunto = TextoPlano.exigir(asunto, 200);
    this.cuerpo = TextoPlano.exigir(cuerpo, 8000);
    this.abiertoEn = abiertoEn;
    this.venceEl = venceEl;
    this.estado = EstadoTicket.ABIERTO;
  }

  public static Ticket rehidratar(
      TicketId id,
      OrganizacionId organizacionId,
      UsuarioId autorId,
      CategoriaId categoriaId,
      Prioridad prioridad,
      String codigo,
      String asunto,
      String cuerpo,
      Instant abiertoEn,
      Instant venceEl,
      EstadoTicket estado,
      UsuarioId asignadoId,
      List<Comentario> comentarios) {
    Ticket ticket =
        new Ticket(
            id,
            organizacionId,
            autorId,
            categoriaId,
            prioridad,
            codigo,
            asunto,
            cuerpo,
            abiertoEn,
            venceEl);
    ticket.estado = estado;
    ticket.asignadoId = asignadoId;
    ticket.comentarios.addAll(comentarios);
    return ticket;
  }

  public static Ticket abrir(
      TicketId id,
      OrganizacionId organizacionId,
      UsuarioId autorId,
      CategoriaId categoriaId,
      Prioridad prioridad,
      String codigo,
      String asunto,
      String cuerpo,
      Instant abiertoEn,
      Instant venceEl) {
    return new Ticket(
        id,
        organizacionId,
        autorId,
        categoriaId,
        prioridad,
        codigo,
        asunto,
        cuerpo,
        abiertoEn,
        venceEl);
  }

  public void transicionar(EstadoTicket destino, Rol rol) {
    if (rol == Rol.CLIENTE) {
      throw new Prohibido();
    }
    if (!permitida(estado, destino)) {
      throw new TransicionNoPermitida();
    }
    estado = destino;
  }

  public void reclasificar(
      Categoria categoria, Prioridad nuevaPrioridad, Rol rol, PoliticaSla politica) {
    if (rol == Rol.CLIENTE) {
      throw new Prohibido();
    }
    if (!categoria.organizacionId().equals(organizacionId)) {
      throw new NoEncontrado();
    }
    if (!reclasificable()) {
      throw new TransicionNoPermitida();
    }
    categoriaId = categoria.id();
    prioridad = nuevaPrioridad;
    venceEl = abiertoEn.plus(politica.primeraRespuesta(categoria.nombre(), nuevaPrioridad));
  }

  /** Un ticket resuelto o cerrado ya no cambia de categoria ni de prioridad. */
  public boolean reclasificable() {
    return estado == EstadoTicket.ABIERTO || estado == EstadoTicket.PENDIENTE;
  }

  public void asignar(UsuarioId agente, Rol rol) {
    if (rol == Rol.CLIENTE) {
      throw new Prohibido();
    }
    asignadoId = agente;
  }

  public Comentario comentar(
      ComentarioId comentarioId,
      UsuarioId autor,
      Rol rol,
      String texto,
      boolean interno,
      Instant ahora) {
    String cuerpoComentario = TextoPlano.exigir(texto, 8000);
    if (interno && rol == Rol.CLIENTE) {
      throw new Prohibido();
    }
    if (rol == Rol.CLIENTE && contarEnlaces(cuerpoComentario) > MAXIMO_ENLACES_DE_CLIENTE) {
      throw new DemasiadosEnlaces();
    }
    Comentario comentario = new Comentario(comentarioId, autor, cuerpoComentario, interno, ahora);
    comentarios.add(comentario);
    return comentario;
  }

  /** Un agente o un admin retira un comentario con un motivo. Nunca se borra. */
  public Comentario ocultarComentario(
      ComentarioId comentarioId,
      UsuarioId actor,
      Rol rol,
      MotivoModeracion motivo,
      Instant ahora) {
    if (rol == Rol.CLIENTE) {
      throw new Prohibido();
    }
    if (motivo == null) {
      throw new TextoInvalido();
    }
    int posicion = posicionDe(comentarioId);
    Comentario actual = comentarios.get(posicion);
    if (actual.retirado()) {
      return actual;
    }
    Comentario oculto = actual.moderado(new Moderacion(motivo, actor, ahora));
    comentarios.set(posicion, oculto);
    return oculto;
  }

  /** Solo un admin devuelve un comentario retirado al hilo. */
  public Comentario restaurarComentario(ComentarioId comentarioId, Rol rol) {
    if (rol != Rol.ADMIN) {
      throw new Prohibido();
    }
    int posicion = posicionDe(comentarioId);
    Comentario restaurado = comentarios.get(posicion).moderado(null);
    comentarios.set(posicion, restaurado);
    return restaurado;
  }

  private int posicionDe(ComentarioId comentarioId) {
    for (int i = 0; i < comentarios.size(); i++) {
      if (comentarios.get(i).id().equals(comentarioId)) {
        return i;
      }
    }
    throw new NoEncontrado();
  }

  private static int contarEnlaces(String texto) {
    return (int) ENLACE.matcher(texto).results().count();
  }

  /** Un cliente solo ve los tickets que abrio; el equipo (agente y admin) ve los de su organizacion. */
  public boolean visiblePara(UsuarioId solicitante, Rol rol) {
    return rol != Rol.CLIENTE || autorId.equals(solicitante);
  }

  /** Lo que ve cada rol: el cliente no ve notas internas ni el texto de lo retirado. */
  public List<Comentario> visiblesPara(Rol rol) {
    if (rol == Rol.CLIENTE) {
      return comentarios.stream()
          .filter(comentario -> !comentario.interno())
          .map(comentario -> comentario.retirado() ? comentario.sinCuerpo() : comentario)
          .toList();
    }
    return List.copyOf(comentarios);
  }

  /** Todos los comentarios tal como se guardan, retirados e internos incluidos. */
  public List<Comentario> todos() {
    return List.copyOf(comentarios);
  }

  public boolean coincide(EstadoTicket estadoFiltro, Prioridad prioridadFiltro, UsuarioId asignado) {
    if (estadoFiltro != null && estado != estadoFiltro) {
      return false;
    }
    if (prioridadFiltro != null && prioridad != prioridadFiltro) {
      return false;
    }
    if (asignado != null && !asignado.equals(asignadoId)) {
      return false;
    }
    return true;
  }

  private static boolean permitida(EstadoTicket desde, EstadoTicket hacia) {
    return switch (desde) {
      case ABIERTO -> hacia == EstadoTicket.PENDIENTE || hacia == EstadoTicket.RESUELTO;
      case PENDIENTE -> hacia == EstadoTicket.ABIERTO || hacia == EstadoTicket.RESUELTO;
      case RESUELTO -> hacia == EstadoTicket.ABIERTO || hacia == EstadoTicket.CERRADO;
      case CERRADO -> false;
    };
  }

  public TicketId id() {
    return id;
  }

  public OrganizacionId organizacionId() {
    return organizacionId;
  }

  public UsuarioId autorId() {
    return autorId;
  }

  public CategoriaId categoriaId() {
    return categoriaId;
  }

  public Prioridad prioridad() {
    return prioridad;
  }

  public String codigo() {
    return codigo;
  }

  public String asunto() {
    return asunto;
  }

  public String cuerpo() {
    return cuerpo;
  }

  public Instant abiertoEn() {
    return abiertoEn;
  }

  public Instant venceEl() {
    return venceEl;
  }

  public EstadoTicket estado() {
    return estado;
  }

  public UsuarioId asignadoId() {
    return asignadoId;
  }
}
