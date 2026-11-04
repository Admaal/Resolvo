package com.admaal.resolvo.infrastructure.persistencia;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UsuarioJpa extends JpaRepository<UsuarioEntity, UUID> {
  Optional<UsuarioEntity> findByEmail(String email);

  List<UsuarioEntity> findByOrganizacionId(UUID organizacionId);
}
