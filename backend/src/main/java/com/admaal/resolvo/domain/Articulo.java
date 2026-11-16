package com.admaal.resolvo.domain;

import java.util.UUID;

public record Articulo(UUID id, OrganizacionId organizacionId, String titulo, String cuerpo) {}
