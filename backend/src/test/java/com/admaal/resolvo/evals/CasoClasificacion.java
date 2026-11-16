package com.admaal.resolvo.evals;

import com.admaal.resolvo.domain.Prioridad;

/** Un ticket de ejemplo con la categoria y la prioridad que una persona le pondria. */
public record CasoClasificacion(
    String id, String asunto, String cuerpo, String categoria, Prioridad prioridad) {}
