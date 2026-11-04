package com.admaal.resolvo.application;

import com.admaal.resolvo.domain.OrganizacionId;
import com.admaal.resolvo.domain.Usuario;
import com.admaal.resolvo.domain.UsuarioId;
import java.util.Optional;

public interface Usuarios {
  Optional<Usuario> porEmail(String email);

  Optional<Usuario> porId(UsuarioId id);

  void guardar(Usuario usuario);
}
