package com.admaal.resolvo.infrastructure.persistencia;

import com.admaal.resolvo.domain.FuenteConocimiento;
import java.util.UUID;

/** Copia de la fuente citada: sobrevive a que reindexar reemplace los fragmentos. */
public record CitaDetalle(int orden, FuenteConocimiento fuente, UUID fuenteId, String titulo) {}
