package com.admaal.resolvo.domain;

import java.time.Instant;

/** Quién ocultó un comentario, cuándo y por qué. El comentario no se borra: solo se retira. */
public record Moderacion(MotivoModeracion motivo, UsuarioId por, Instant en) {}
