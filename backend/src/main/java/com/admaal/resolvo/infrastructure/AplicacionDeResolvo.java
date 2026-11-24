package com.admaal.resolvo.infrastructure;

import com.admaal.resolvo.application.AbrirTicket;
import com.admaal.resolvo.application.AbrirTicketComando;
import com.admaal.resolvo.application.AsignarTicket;
import com.admaal.resolvo.application.Categorias;
import com.admaal.resolvo.application.CerrarSesion;
import com.admaal.resolvo.application.CodigosDeTicket;
import com.admaal.resolvo.application.ComentarTicket;
import com.admaal.resolvo.application.ConsultaTickets;
import com.admaal.resolvo.application.ConsultarTicket;
import com.admaal.resolvo.application.EmisorDeTokens;
import com.admaal.resolvo.application.IniciarSesion;
import com.admaal.resolvo.application.ListarTickets;
import com.admaal.resolvo.application.ModerarComentario;
import com.admaal.resolvo.application.Pagina;
import com.admaal.resolvo.application.PublicadorEventos;
import com.admaal.resolvo.application.RefrescarSesion;
import com.admaal.resolvo.application.Reloj;
import com.admaal.resolvo.application.SesionIniciada;
import com.admaal.resolvo.application.SesionesRefresh;
import com.admaal.resolvo.application.TicketRepositorio;
import com.admaal.resolvo.application.TransicionarTicket;
import com.admaal.resolvo.application.Usuarios;
import com.admaal.resolvo.application.VerificadorContrasena;
import com.admaal.resolvo.domain.Categoria;
import com.admaal.resolvo.domain.Comentario;
import com.admaal.resolvo.domain.ComentarioId;
import com.admaal.resolvo.domain.CredencialesInvalidas;
import com.admaal.resolvo.domain.EstadoTicket;
import com.admaal.resolvo.domain.MotivoModeracion;
import com.admaal.resolvo.domain.OrganizacionId;
import com.admaal.resolvo.domain.Rol;
import com.admaal.resolvo.domain.Ticket;
import com.admaal.resolvo.domain.TicketId;
import com.admaal.resolvo.domain.Usuario;
import com.admaal.resolvo.domain.UsuarioId;
import com.admaal.resolvo.infrastructure.persistencia.CategoriaJpa;
import com.admaal.resolvo.infrastructure.persistencia.Mapeo;
import com.admaal.resolvo.infrastructure.persistencia.UsuarioJpa;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AplicacionDeResolvo {

  private final Reloj reloj;
  private final Usuarios usuarios;
  private final VerificadorContrasena verificador;
  private final EmisorDeTokens tokens;
  private final SesionesRefresh sesiones;
  private final CodigosDeTicket codigos;
  private final TicketRepositorio tickets;
  private final Categorias categorias;
  private final CategoriaJpa categoriaJpa;
  private final UsuarioJpa usuarioJpa;
  private final PublicadorEventos eventos;

  public AplicacionDeResolvo(
      Reloj reloj,
      Usuarios usuarios,
      VerificadorContrasena verificador,
      EmisorDeTokens tokens,
      SesionesRefresh sesiones,
      CodigosDeTicket codigos,
      TicketRepositorio tickets,
      Categorias categorias,
      CategoriaJpa categoriaJpa,
      UsuarioJpa usuarioJpa,
      PublicadorEventos eventos) {
    this.eventos = eventos;
    this.reloj = reloj;
    this.usuarios = usuarios;
    this.verificador = verificador;
    this.tokens = tokens;
    this.sesiones = sesiones;
    this.codigos = codigos;
    this.tickets = tickets;
    this.categorias = categorias;
    this.categoriaJpa = categoriaJpa;
    this.usuarioJpa = usuarioJpa;
  }

  @Transactional(noRollbackFor = CredencialesInvalidas.class)
  public SesionIniciada entrar(String email, String contrasena) {
    return new IniciarSesion(reloj, usuarios, verificador, tokens, sesiones).ejecutar(email, contrasena);
  }

  @Transactional(noRollbackFor = CredencialesInvalidas.class)
  public SesionIniciada refrescar(String token) {
    return new RefrescarSesion(reloj, usuarios, tokens, sesiones).ejecutar(token);
  }

  @Transactional
  public void cerrar(String token) {
    new CerrarSesion(sesiones).ejecutar(token);
  }

  @Transactional
  public Ticket abrir(AbrirTicketComando comando) {
    return new AbrirTicket(reloj, codigos, tickets, categorias).ejecutar(comando);
  }

  @Transactional(readOnly = true)
  public Ticket consultar(
      OrganizacionId organizacionId, TicketId ticketId, UsuarioId solicitante, Rol rol) {
    return new ConsultarTicket(tickets).ejecutar(organizacionId, ticketId, solicitante, rol);
  }

  @Transactional(readOnly = true)
  public Ticket consultarPorCodigo(
      OrganizacionId organizacionId, String codigo, UsuarioId solicitante, Rol rol) {
    return new ConsultarTicket(tickets).porCodigo(organizacionId, codigo, solicitante, rol);
  }

  @Transactional(readOnly = true)
  public List<Comentario> comentarios(
      OrganizacionId organizacionId, TicketId ticketId, UsuarioId solicitante, Rol rol) {
    return new ConsultarTicket(tickets).comentarios(organizacionId, ticketId, solicitante, rol);
  }

  @Transactional(readOnly = true)
  public Pagina<Ticket> listar(ConsultaTickets consulta) {
    return new ListarTickets(tickets).ejecutar(consulta);
  }

  @Transactional
  public Ticket transicionar(
      OrganizacionId organizacionId, TicketId ticketId, EstadoTicket destino, Rol rol) {
    return new TransicionarTicket(tickets, eventos, reloj).ejecutar(organizacionId, ticketId, destino, rol);
  }

  @Transactional
  public Ticket asignar(
      OrganizacionId organizacionId, TicketId ticketId, UsuarioId asignadoId, Rol actor) {
    return new AsignarTicket(tickets, usuarios).ejecutar(organizacionId, ticketId, asignadoId, actor);
  }

  @Transactional
  public Comentario comentar(
      OrganizacionId organizacionId,
      TicketId ticketId,
      UsuarioId autorId,
      Rol rol,
      String cuerpo,
      boolean interno) {
    return new ComentarTicket(tickets, reloj)
        .ejecutar(organizacionId, ticketId, autorId, rol, cuerpo, interno);
  }

  @Transactional
  public Comentario ocultarComentario(
      OrganizacionId organizacionId,
      TicketId ticketId,
      ComentarioId comentarioId,
      UsuarioId actor,
      Rol rol,
      MotivoModeracion motivo) {
    return new ModerarComentario(tickets, eventos, reloj)
        .ocultar(organizacionId, ticketId, comentarioId, actor, rol, motivo);
  }

  @Transactional
  public Comentario restaurarComentario(
      OrganizacionId organizacionId, TicketId ticketId, ComentarioId comentarioId, Rol rol) {
    return new ModerarComentario(tickets, eventos, reloj)
        .restaurar(organizacionId, ticketId, comentarioId, rol);
  }

  @Transactional(readOnly = true)
  public List<Categoria> categoriasDe(OrganizacionId organizacionId) {
    return categoriaJpa.findByOrganizacionId(organizacionId.valor()).stream()
        .map(Mapeo::categoria)
        .toList();
  }

  @Transactional(readOnly = true)
  public List<Usuario> usuariosDe(OrganizacionId organizacionId) {
    return usuarioJpa.findByOrganizacionId(organizacionId.valor()).stream().map(Mapeo::usuario).toList();
  }
}
