package com.admaal.resolvo.infrastructure.persistencia;

import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface BonoJpa extends JpaRepository<BonoEntity, String> {

  @Modifying(clearAutomatically = true, flushAutomatically = true)
  @Query("update BonoEntity b set b.revocado = true where b.familia = :familia")
  void revocarFamilia(UUID familia);
}
