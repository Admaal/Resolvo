package com.admaal.resolvo.infrastructure.ai;

import com.admaal.resolvo.application.Embebedor;
import com.admaal.resolvo.application.IaNoDisponible;
import com.admaal.resolvo.domain.Embedding;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.embedding.EmbeddingModel;

public class EmbebedorSpringAi implements Embebedor {

  private static final Logger LOG = LoggerFactory.getLogger(EmbebedorSpringAi.class);

  private final EmbeddingModel modelo;
  private final String nombreDelModelo;

  public EmbebedorSpringAi(EmbeddingModel modelo, String nombreDelModelo) {
    this.modelo = modelo;
    this.nombreDelModelo = nombreDelModelo;
  }

  @Override
  public String modelo() {
    return nombreDelModelo;
  }

  @Override
  public Embedding embeber(String texto) {
    try {
      return new Embedding(modelo.embed(texto));
    } catch (RuntimeException error) {
      LOG.warn("Gemini embeddings: {}", FalloDelProveedor.resumen(error));
      throw FalloDelProveedor.de(error);
    }
  }
}
