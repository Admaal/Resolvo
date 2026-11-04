package com.admaal.resolvo.application.soporte;

import com.admaal.resolvo.application.Usuarios;
import com.admaal.resolvo.domain.Usuario;
import com.admaal.resolvo.domain.UsuarioId;
import java.util.ArrayList;
import java.util.Optional;

public final class UsuariosEnMemoria implements Usuarios {

  private final ArrayList<Usuario> guardados = new ArrayList<>();
  private int vecesGuardado;

  @Override
  public Optional<Usuario> porEmail(String email) {
    return guardados.stream()
        .filter(usuario -> usuario.email().equals(email))
        .findFirst()
        .map(Usuario::copia);
  }

  @Override
  public Optional<Usuario> porId(UsuarioId id) {
    return guardados.stream().filter(usuario -> usuario.id().equals(id)).findFirst().map(Usuario::copia);
  }

  @Override
  public void guardar(Usuario usuario) {
    vecesGuardado++;
    guardados.removeIf(existente -> existente.id().equals(usuario.id()));
    guardados.add(usuario.copia());
  }

  public int vecesGuardado() {
    return vecesGuardado;
  }
}
