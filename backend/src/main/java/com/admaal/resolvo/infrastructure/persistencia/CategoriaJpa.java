package com.admaal.resolvo.infrastructure.persistencia;

import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CategoriaJpa extends JpaRepository<CategoriaEntity, UUID> {
  List<CategoriaEntity> findByOrganizacionId(UUID organizacionId);
}
