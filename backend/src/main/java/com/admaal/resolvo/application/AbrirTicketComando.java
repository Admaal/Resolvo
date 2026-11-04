package com.admaal.resolvo.application;

import com.admaal.resolvo.domain.CategoriaId;
import com.admaal.resolvo.domain.OrganizacionId;
import com.admaal.resolvo.domain.Prioridad;
import com.admaal.resolvo.domain.Rol;
import com.admaal.resolvo.domain.UsuarioId;

public record AbrirTicketComando(
    OrganizacionId organizacionId,
    UsuarioId autorId,
    Rol rol,
    CategoriaId categoriaId,
    Prioridad prioridad,
    String asunto,
    String cuerpo) {}
