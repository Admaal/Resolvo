package com.admaal.resolvo.application.soporte;

import com.admaal.resolvo.application.Embebedor;
import com.admaal.resolvo.domain.Embedding;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public final class EmbebedorFalso implements Embebedor {

  private final List<String> textos = new ArrayList<>();

  @Override
  public String modelo() {
    return "falso-768";
  }

  @Override
  public Embedding embeber(String texto) {
    textos.add(texto);
    Random azar = new Random(texto.hashCode());
    float[] valores = new float[Embedding.DIMENSION];
    for (int i = 0; i < valores.length; i++) {
      valores[i] = azar.nextFloat();
    }
    return new Embedding(valores);
  }

  public int llamadas() {
    return textos.size();
  }

  public List<String> textos() {
    return List.copyOf(textos);
  }
}
