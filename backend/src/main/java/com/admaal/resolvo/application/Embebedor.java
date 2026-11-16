package com.admaal.resolvo.application;

import com.admaal.resolvo.domain.Embedding;

public interface Embebedor {
  String modelo();

  Embedding embeber(String texto);
}
