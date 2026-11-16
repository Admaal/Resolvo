package com.admaal.resolvo.infrastructure.web;

import com.admaal.resolvo.domain.Articulo;
import com.admaal.resolvo.infrastructure.AplicacionDeConocimiento;
import java.util.UUID;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class ConocimientoControlador {

  private final AplicacionDeConocimiento conocimiento;

  public ConocimientoControlador(AplicacionDeConocimiento conocimiento) {
    this.conocimiento = conocimiento;
  }

  @GetMapping("/api/articulos/{id}")
  ArticuloRespuesta articulo(@PathVariable UUID id) {
    Actor actor = Actor.actual();
    Articulo articulo = conocimiento.articulo(actor.organizacionId(), id, actor.rol());
    return new ArticuloRespuesta(articulo.id(), articulo.titulo(), articulo.cuerpo());
  }

  @PostMapping("/api/conocimiento/reindexar")
  ResponseEntity<Void> reindexar() {
    Actor actor = Actor.actual();
    conocimiento.reindexar(actor.organizacionId(), actor.rol());
    return ResponseEntity.noContent().build();
  }

  public record ArticuloRespuesta(UUID id, String titulo, String cuerpo) {}
}
