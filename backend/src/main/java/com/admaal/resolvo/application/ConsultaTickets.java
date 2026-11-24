package com.admaal.resolvo.application;

import com.admaal.resolvo.domain.EstadoTicket;
import com.admaal.resolvo.domain.OrganizacionId;
import com.admaal.resolvo.domain.Prioridad;
import com.admaal.resolvo.domain.Rol;
import com.admaal.resolvo.domain.UsuarioId;

public record ConsultaTickets(
    OrganizacionId organizacionId,
    UsuarioId solicitanteId,
    Rol rol,
    EstadoTicket estado,
    Prioridad prioridad,
    UsuarioId asignadoId,
    int pagina,
    int tamano) {}
