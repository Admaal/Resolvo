package com.admaal.resolvo.infrastructure.web;

import com.admaal.resolvo.domain.OrganizacionId;
import com.admaal.resolvo.domain.Rol;
import com.admaal.resolvo.domain.UsuarioId;
import java.util.UUID;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;

public record Actor(UsuarioId id, OrganizacionId organizacionId, Rol rol) {

  public static Actor actual() {
    Jwt jwt = (Jwt) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
    return new Actor(
        new UsuarioId(UUID.fromString(jwt.getSubject())),
        new OrganizacionId(UUID.fromString(jwt.getClaimAsString("organizacionId"))),
        Rol.valueOf(jwt.getClaimAsString("rol")));
  }
}
