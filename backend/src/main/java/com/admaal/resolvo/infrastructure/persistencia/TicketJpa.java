package com.admaal.resolvo.infrastructure.persistencia;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface TicketJpa extends JpaRepository<TicketEntity, UUID> {

  @Query(
      """
      select distinct t from TicketEntity t
      left join fetch t.comentarios
      where t.organizacionId = :organizacion and t.id = :id
      """)
  Optional<TicketEntity> porIdEnOrganizacion(UUID organizacion, UUID id);

  @Query(
      """
      select distinct t from TicketEntity t
      left join fetch t.comentarios
      where t.organizacionId = :organizacion and t.codigo = :codigo
      """)
  Optional<TicketEntity> porCodigoEnOrganizacion(UUID organizacion, String codigo);

  @Query(
      """
      select distinct t from TicketEntity t
      left join fetch t.comentarios
      where t.organizacionId = :organizacion
      """)
  List<TicketEntity> deLaOrganizacion(UUID organizacion);
}
