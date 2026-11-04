package com.admaal.resolvo.application;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.admaal.resolvo.application.soporte.RelojManual;
import com.admaal.resolvo.application.soporte.SesionesEnMemoria;
import com.admaal.resolvo.application.soporte.UsuariosEnMemoria;
import com.admaal.resolvo.domain.CredencialesInvalidas;
import com.admaal.resolvo.domain.CuentaBloqueada;
import com.admaal.resolvo.domain.OrganizacionId;
import com.admaal.resolvo.domain.Rol;
import com.admaal.resolvo.domain.Usuario;
import com.admaal.resolvo.domain.UsuarioId;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class IniciarSesionTest {

  private final Instant ahora = Instant.parse("2026-11-04T10:00:00Z");
  private final AtomicReference<Instant> vence = new AtomicReference<>();

  @Test
  void credencialesMalasNoAbrenSesion() {
    UsuariosEnMemoria usuarios = usuariosCon("cliente@acme.test");
    SesionesEnMemoria sesiones = new SesionesEnMemoria();
    IniciarSesion caso = caso(new RelojManual(ahora), usuarios, sesiones);

    assertThatThrownBy(() -> caso.ejecutar("cliente@acme.test", "mala"))
        .isInstanceOf(CredencialesInvalidas.class);
    assertThatThrownBy(() -> caso.ejecutar("nadie@acme.test", "demo-resolvo"))
        .isInstanceOf(CredencialesInvalidas.class);
    assertThat(sesiones.porToken("refresh-1")).isEmpty();
  }

  @Test
  void bloqueaElEmailTrasCincoFallos() {
    RelojManual reloj = new RelojManual(ahora);
    UsuariosEnMemoria usuarios = usuariosCon("cliente@acme.test");
    IniciarSesion caso = caso(reloj, usuarios, new SesionesEnMemoria());

    for (int intento = 0; intento < 4; intento++) {
      assertThatThrownBy(() -> caso.ejecutar("cliente@acme.test", "mala"))
          .isInstanceOf(CredencialesInvalidas.class);
    }
    assertThatThrownBy(() -> caso.ejecutar("cliente@acme.test", "mala"))
        .isInstanceOf(CuentaBloqueada.class);
    assertThatThrownBy(() -> caso.ejecutar("cliente@acme.test", "demo-resolvo"))
        .isInstanceOf(CuentaBloqueada.class);

    reloj.avanzar(Duration.ofMinutes(14));
    assertThatThrownBy(() -> caso.ejecutar("cliente@acme.test", "demo-resolvo"))
        .isInstanceOf(CuentaBloqueada.class);

    reloj.avanzar(Duration.ofMinutes(1));
    SesionIniciada sesion = caso.ejecutar("cliente@acme.test", "demo-resolvo");
    assertThat(sesion.access()).isEqualTo("access");
    assertThat(sesion.refresh()).isEqualTo("refresh-1");
    assertThat(vence.get()).isEqualTo(reloj.ahora().plus(Duration.ofMinutes(15)));

    for (int intento = 0; intento < 4; intento++) {
      assertThatThrownBy(() -> caso.ejecutar("cliente@acme.test", "mala"))
          .isInstanceOf(CredencialesInvalidas.class);
    }
  }

  private IniciarSesion caso(Reloj reloj, Usuarios usuarios, SesionesRefresh sesiones) {
    return new IniciarSesion(
        reloj,
        usuarios,
        (plana, hash) -> "demo-resolvo".equals(plana) && "hash".equals(hash),
        (usuario, caduca) -> {
          vence.set(caduca);
          return "access";
        },
        sesiones);
  }

  private static UsuariosEnMemoria usuariosCon(String email) {
    UsuariosEnMemoria usuarios = new UsuariosEnMemoria();
    usuarios.guardar(
        Usuario.nuevo(
            new UsuarioId(UUID.randomUUID()),
            new OrganizacionId(UUID.randomUUID()),
            email,
            Rol.CLIENTE,
            "hash"));
    return usuarios;
  }
}
