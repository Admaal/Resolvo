package com.admaal.resolvo.application;

import static org.assertj.core.api.Assertions.assertThat;
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

class RefrescarSesionTest {

  @Test
  void reutilizarElRefreshNoEmiteOtraSesion() {
    Instant ahora = Instant.parse("2026-11-04T10:00:00Z");
    UsuariosEnMemoria usuarios = new UsuariosEnMemoria();
    Usuario usuario =
        Usuario.nuevo(
            new UsuarioId(UUID.randomUUID()),
            new OrganizacionId(UUID.randomUUID()),
            "cliente@acme.test",
            Rol.CLIENTE,
            "hash");
    usuarios.guardar(usuario);
    SesionesEnMemoria sesiones = new SesionesEnMemoria();
    IniciarSesion alta =
        new IniciarSesion(
            new RelojManual(ahora),
            usuarios,
            (plana, hash) -> "demo-resolvo".equals(plana) && "hash".equals(hash),
            (ignorado, vence) -> "access",
            sesiones);
    SesionIniciada inicial = alta.ejecutar("cliente@acme.test", "demo-resolvo");
    RefrescarSesion refrescar =
        new RefrescarSesion(
            new RelojManual(ahora), usuarios, (ignorado, vence) -> "access-2", sesiones);

    SesionIniciada rotada = refrescar.ejecutar(inicial.refresh());
    SesionIniciada siguiente = refrescar.ejecutar(rotada.refresh());

    assertThat(rotada.refresh()).isEqualTo("refresh-2");
    assertThat(siguiente.refresh()).isEqualTo("refresh-3");
    assertThatThrownBy(() -> refrescar.ejecutar(rotada.refresh()))
        .isInstanceOf(CredencialesInvalidas.class);
    assertThatThrownBy(() -> refrescar.ejecutar(siguiente.refresh()))
        .isInstanceOf(CredencialesInvalidas.class);
  }
}
