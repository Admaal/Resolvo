package com.admaal.resolvo.application;

import com.admaal.resolvo.domain.Prioridad;

/** Lo que devuelve el clasificador: el nombre de la categoria, tal como el modelo la escribio. */
public record ClasificacionPropuesta(String categoria, Prioridad prioridad, String motivo) {}
