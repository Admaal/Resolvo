package com.admaal.resolvo.infrastructure.persistencia;

import com.admaal.resolvo.application.Usuarios;
import com.admaal.resolvo.domain.Usuario;
import com.admaal.resolvo.domain.UsuarioId;
import java.util.Optional;
import org.springframework.stereotype.Repository;

@Repository
public class UsuariosJpa implements Usuarios {

  private final UsuarioJpa usuarios;

  public UsuariosJpa(UsuarioJpa usuarios) {
    this.usuarios = usuarios;
  }

  @Override
  public Optional<Usuario> porEmail(String email) {
    return usuarios.findByEmail(email).map(Mapeo::usuario);
  }

  @Override
  public Optional<Usuario> porId(UsuarioId id) {
    return usuarios.findById(id.valor()).map(Mapeo::usuario);
  }

  @Override
  public void guardar(Usuario usuario) {
    UsuarioEntity fila = usuarios.findById(usuario.id().valor()).orElseGet(UsuarioEntity::new);
    Mapeo.copiar(usuario, fila);
    usuarios.save(fila);
  }
}
