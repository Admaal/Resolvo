package com.admaal.resolvo.infrastructure.web;

import com.admaal.resolvo.domain.Categoria;
import com.admaal.resolvo.domain.Usuario;
import com.admaal.resolvo.infrastructure.AplicacionDeResolvo;
import java.util.List;
import java.util.UUID;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class CatalogoControlador {

  private final AplicacionDeResolvo aplicacion;

  public CatalogoControlador(AplicacionDeResolvo aplicacion) {
    this.aplicacion = aplicacion;
  }

  @GetMapping("/api/categorias")
  List<CategoriaRespuesta> categorias() {
    Actor actor = Actor.actual();
    return aplicacion.categoriasDe(actor.organizacionId()).stream().map(CategoriaRespuesta::de).toList();
  }

  @GetMapping("/api/usuarios")
  List<UsuarioRespuesta> usuarios() {
    Actor actor = Actor.actual();
    return aplicacion.usuariosDe(actor.organizacionId()).stream().map(UsuarioRespuesta::de).toList();
  }

  public record CategoriaRespuesta(UUID id, String nombre) {
    static CategoriaRespuesta de(Categoria categoria) {
      return new CategoriaRespuesta(categoria.id().valor(), categoria.nombre());
    }
  }

  public record UsuarioRespuesta(UUID id, String email, String rol) {
    static UsuarioRespuesta de(Usuario usuario) {
      return new UsuarioRespuesta(usuario.id().valor(), usuario.email(), usuario.rol().name());
    }
  }
}
