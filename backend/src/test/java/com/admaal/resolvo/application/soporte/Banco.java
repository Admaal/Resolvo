package com.admaal.resolvo.application.soporte;

import com.admaal.resolvo.application.AbrirTicket;
import com.admaal.resolvo.application.AbrirTicketComando;
import com.admaal.resolvo.domain.Categoria;
import com.admaal.resolvo.domain.CategoriaId;
import com.admaal.resolvo.domain.OrganizacionId;
import com.admaal.resolvo.domain.Prioridad;
import com.admaal.resolvo.domain.Rol;
import com.admaal.resolvo.domain.Ticket;
import com.admaal.resolvo.domain.UsuarioId;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

public final class Banco {

  private Banco() {}

  public static Ticket abrir(
      RepositorioEnMemoria tickets,
      OrganizacionId organizacion,
      UsuarioId autor,
      Prioridad prioridad,
      Instant ahora) {
    CategoriaId categoria = new CategoriaId(UUID.randomUUID());
    return new AbrirTicket(
            () -> ahora,
            ignorada -> "RS-1",
            tickets,
            (org, id) -> Optional.of(new Categoria(id, org, "Incidencia")))
        .ejecutar(
            new AbrirTicketComando(
                organizacion,
                autor,
                Rol.CLIENTE,
                categoria,
                prioridad,
                "Asunto valido",
                "Cuerpo valido"));
  }
}
