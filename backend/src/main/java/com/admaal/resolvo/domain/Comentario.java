package com.admaal.resolvo.domain;

import java.time.Instant;

public record Comentario(
    ComentarioId id, UsuarioId autorId, String cuerpo, boolean interno, Instant creadoEn) {}
