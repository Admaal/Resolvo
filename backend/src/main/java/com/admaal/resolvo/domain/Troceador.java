package com.admaal.resolvo.domain;

import java.util.ArrayList;
import java.util.List;

public final class Troceador {

  public static final int TAMANO = 800;
  public static final int SOLAPE = 100;

  private Troceador() {}

  public static List<String> trocear(String texto) {
    if (texto != null && !texto.isBlank() && texto.strip().length() <= TAMANO) {
      return List.of(texto.strip());
    }
    List<String> palabras = palabras(texto);
    List<String> fragmentos = new ArrayList<>();
    int inicio = 0;
    while (inicio < palabras.size()) {
      int fin = inicio;
      int largo = 0;
      while (fin < palabras.size() && cabe(largo, palabras.get(fin))) {
        largo += (largo == 0 ? 0 : 1) + palabras.get(fin).length();
        fin++;
      }
      fragmentos.add(String.join(" ", palabras.subList(inicio, fin)));
      if (fin >= palabras.size()) {
        break;
      }
      inicio = inicioDelSolape(palabras, inicio, fin);
    }
    return fragmentos;
  }

  private static boolean cabe(int largo, String palabra) {
    return largo + (largo == 0 ? 0 : 1) + palabra.length() <= TAMANO;
  }

  private static int inicioDelSolape(List<String> palabras, int inicio, int fin) {
    int atras = fin;
    int solape = 0;
    while (atras > inicio + 1 && solape + palabras.get(atras - 1).length() + 1 <= SOLAPE) {
      solape += palabras.get(atras - 1).length() + 1;
      atras--;
    }
    return atras;
  }

  private static List<String> palabras(String texto) {
    List<String> resultado = new ArrayList<>();
    if (texto == null || texto.isBlank()) {
      return resultado;
    }
    for (String palabra : texto.trim().split("\s+")) {
      for (int desde = 0; desde < palabra.length(); desde += TAMANO) {
        resultado.add(palabra.substring(desde, Math.min(palabra.length(), desde + TAMANO)));
      }
    }
    return resultado;
  }
}
