package com.admaal.resolvo.evals;

/** Una consulta y el titulo del articulo de la semilla que la responde; sin articulo, no hay respuesta. */
public record CasoRag(String id, String asunto, String cuerpo, String articulo) {

  public boolean tieneRespuesta() {
    return articulo != null;
  }
}
