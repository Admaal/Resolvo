package com.admaal.resolvo.evals;

/**
 * Una consulta y la fuente de la semilla que la responde: el titulo de un articulo o el codigo de un
 * ticket resuelto. Sin ninguna de las dos, no hay respuesta.
 */
public record CasoRag(String id, String asunto, String cuerpo, String articulo, String ticket) {

  public boolean tieneRespuesta() {
    return articulo != null || ticket != null;
  }
}
