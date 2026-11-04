package com.admaal.resolvo.domain;

public final class TextoPlano {

  private TextoPlano() {}

  public static String exigir(String valor, int maximo) {
    if (valor == null || valor.isBlank()) {
      throw new TextoInvalido();
    }
    if (valor.length() > maximo) {
      throw new TextoInvalido();
    }
    return valor;
  }
}
