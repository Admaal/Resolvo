package com.admaal.resolvo.infrastructure.persistencia;

import com.admaal.resolvo.domain.Categoria;
import com.admaal.resolvo.domain.CategoriaId;
import com.admaal.resolvo.domain.Comentario;
import com.admaal.resolvo.domain.ComentarioId;
import com.admaal.resolvo.domain.EstadoTicket;
import com.admaal.resolvo.domain.OrganizacionId;
import com.admaal.resolvo.domain.Rol;
import com.admaal.resolvo.domain.Ticket;
import com.admaal.resolvo.domain.TicketId;
import com.admaal.resolvo.domain.Usuario;
import com.admaal.resolvo.domain.UsuarioId;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class Mapeo {

  private Mapeo() {}

  public static Usuario usuario(UsuarioEntity fila) {
    return new Usuario(
        new UsuarioId(fila.getId()),
        new OrganizacionId(fila.getOrganizacionId()),
        fila.getEmail(),
        fila.getRol(),
        fila.getHashContrasena(),
        fila.getFallosSeguidos(),
        fila.getBloqueadoHasta());
  }

  static void copiar(Usuario usuario, UsuarioEntity fila) {
    fila.setId(usuario.id().valor());
    fila.setOrganizacionId(usuario.organizacionId().valor());
    fila.setEmail(usuario.email());
    fila.setRol(usuario.rol());
    fila.setHashContrasena(usuario.hashContrasena());
    fila.setFallosSeguidos(usuario.fallosSeguidos());
    fila.setBloqueadoHasta(usuario.bloqueadoHasta());
  }

  public static Categoria categoria(CategoriaEntity fila) {
    return new Categoria(
        new CategoriaId(fila.getId()), new OrganizacionId(fila.getOrganizacionId()), fila.getNombre());
  }

  static Ticket ticket(TicketEntity fila) {
    List<Comentario> comentarios = new ArrayList<>();
    for (ComentarioEntity comentario : fila.getComentarios()) {
      comentarios.add(
          new Comentario(
              new ComentarioId(comentario.getId()),
              new UsuarioId(comentario.getAutorId()),
              comentario.getCuerpo(),
              comentario.isInterno(),
              comentario.getCreadoEn()));
    }
    return Ticket.rehidratar(
        new TicketId(fila.getId()),
        new OrganizacionId(fila.getOrganizacionId()),
        new UsuarioId(fila.getAutorId()),
        new CategoriaId(fila.getCategoriaId()),
        fila.getPrioridad(),
        fila.getCodigo(),
        fila.getAsunto(),
        fila.getCuerpo(),
        fila.getAbiertoEn(),
        fila.getVenceEl(),
        fila.getEstado(),
        fila.getAsignadoId() == null ? null : new UsuarioId(fila.getAsignadoId()),
        comentarios);
  }

  static void copiar(Ticket ticket, TicketEntity fila) {
    fila.setId(ticket.id().valor());
    fila.setOrganizacionId(ticket.organizacionId().valor());
    fila.setAutorId(ticket.autorId().valor());
    fila.setCategoriaId(ticket.categoriaId().valor());
    fila.setPrioridad(ticket.prioridad());
    fila.setCodigo(ticket.codigo());
    fila.setAsunto(ticket.asunto());
    fila.setCuerpo(ticket.cuerpo());
    fila.setAbiertoEn(ticket.abiertoEn());
    fila.setVenceEl(ticket.venceEl());
    fila.setEstado(ticket.estado() == null ? EstadoTicket.ABIERTO : ticket.estado());
    fila.setAsignadoId(ticket.asignadoId() == null ? null : ticket.asignadoId().valor());
    Map<UUID, ComentarioEntity> existentes = new HashMap<>();
    for (ComentarioEntity comentario : fila.getComentarios()) {
      existentes.put(comentario.getId(), comentario);
    }
    List<ComentarioEntity> siguientes = new ArrayList<>();
    for (Comentario comentario : ticket.visiblesPara(Rol.ADMIN)) {
      ComentarioEntity destino = existentes.getOrDefault(comentario.id().valor(), new ComentarioEntity());
      destino.setId(comentario.id().valor());
      destino.setAutorId(comentario.autorId().valor());
      destino.setCuerpo(comentario.cuerpo());
      destino.setInterno(comentario.interno());
      destino.setCreadoEn(comentario.creadoEn());
      siguientes.add(destino);
    }
    fila.getComentarios().clear();
    fila.getComentarios().addAll(siguientes);
  }
}
