package com.admaal.resolvo.application;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.admaal.resolvo.application.soporte.RelojManual;
import com.admaal.resolvo.application.soporte.SesionesEnMemoria;
import com.admaal.resolvo.application.soporte.UsuariosEnMemoria;
import com.admaal.resolvo.domain.CredencialesInvalidas;
import com.admaal.resolvo.domain.OrganizacionId;
import com.admaal.resolvo.domain.Rol;
import com.admaal.resolvo.domain.Usuario;
import com.admaal.resolvo.domain.UsuarioId;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class CerrarSesionTest {

  @Test
  void elLogoutInvalidaElRefresh() {
    Instant ahora = Instant.parse("2026-11-04T10:00:00Z");
    UsuariosEnMemoria usuarios = new UsuariosEnMemoria();
    usuarios.guardar(
        Usuario.nuevo(
            new UsuarioId(UUID.randomUUID()),
            new OrganizacionId(UUID.randomUUID()),
            "cliente@acme.test",
            Rol.CLIENTE,
            "hash"));
    SesionesEnMemoria sesiones = new SesionesEnMemoria();
    SesionIniciada sesion =
        new IniciarSesion(
                new RelojManual(ahora),
                usuarios,
                (plana, hash) -> "demo-resolvo".equals(plana) && "hash".equals(hash),
                (ignorado, vence) -> "access",
                sesiones)
            .ejecutar("cliente@acme.test", "demo-resolvo");

    new CerrarSesion(sesiones).ejecutar(sesion.refresh());

    assertThatThrownBy(
            () ->
                new RefrescarSesion(
                        new RelojManual(ahora), usuarios, (ignorado, vence) -> "access", sesiones)
                    .ejecutar(sesion.refresh()))
        .isInstanceOf(CredencialesInvalidas.class);
  }
}
