package com.admaal.resolvo.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.admaal.resolvo.application.soporte.RelojManual;
import com.admaal.resolvo.application.soporte.SugerenciasEnMemoria;
import com.admaal.resolvo.domain.EstadoSugerencia;
import com.admaal.resolvo.domain.NoEncontrado;
import com.admaal.resolvo.domain.OrganizacionId;
import com.admaal.resolvo.domain.Prohibido;
import com.admaal.resolvo.domain.Rol;
import com.admaal.resolvo.domain.Sugerencia;
import com.admaal.resolvo.domain.SugerenciaId;
import com.admaal.resolvo.domain.TicketId;
import com.admaal.resolvo.domain.UsuarioId;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class DescartarSugerenciaTest {

  private static final Instant AHORA = Instant.parse("2026-11-16T10:00:00Z");

  private final OrganizacionId acme = new OrganizacionId(UUID.randomUUID());
  private final UsuarioId agente = new UsuarioId(UUID.randomUUID());
  private final SugerenciasEnMemoria sugerencias = new SugerenciasEnMemoria();
  private final TicketId ticketId = TicketId.nuevo();
  private final Sugerencia sugerencia =
      Sugerencia.crear(
          SugerenciaId.nuevo(), acme, ticketId, "Borrador", "m", AHORA, List.of(), Set.of());
  private final DescartarSugerencia caso =
      new DescartarSugerencia(sugerencias, new RelojManual(AHORA));

  DescartarSugerenciaTest() {
    sugerencias.guardar(sugerencia);
  }

  @Test
  void descartaYPermiteOtraNueva() {
    caso.ejecutar(acme, sugerencia.id(), agente, Rol.AGENTE);

    assertThat(sugerencia.estado()).isEqualTo(EstadoSugerencia.DESCARTADA);
    assertThat(sugerencias.pendienteDe(acme, ticketId)).isEmpty();
  }

  @Test
  void clienteRecibe403() {
    assertThatThrownBy(() -> caso.ejecutar(acme, sugerencia.id(), agente, Rol.CLIENTE))
        .isInstanceOf(Prohibido.class);

    assertThat(sugerencia.estado()).isEqualTo(EstadoSugerencia.PENDIENTE);
  }

  @Test
  void sugerenciaDeOtraOrgEs404() {
    OrganizacionId otra = new OrganizacionId(UUID.randomUUID());

    assertThatThrownBy(() -> caso.ejecutar(otra, sugerencia.id(), agente, Rol.AGENTE))
        .isInstanceOf(NoEncontrado.class);
  }

  @Test
  void guardaLaSugerenciaDescartada() {
    int antes = sugerencias.vecesGuardado();

    caso.ejecutar(acme, sugerencia.id(), agente, Rol.AGENTE);

    assertThat(sugerencias.vecesGuardado()).isEqualTo(antes + 1);
  }
}
