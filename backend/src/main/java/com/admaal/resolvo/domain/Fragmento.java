package com.admaal.resolvo.domain;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.UUID;

public record Fragmento(
    FragmentoId id,
    OrganizacionId organizacionId,
    FuenteConocimiento fuente,
    UUID fuenteId,
    String titulo,
    String texto) {

  public static String hashDe(String texto) {
    try {
      byte[] resumen =
          MessageDigest.getInstance("SHA-256").digest(texto.getBytes(StandardCharsets.UTF_8));
      return HexFormat.of().formatHex(resumen);
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException(e);
    }
  }

  public String hash() {
    return hashDe(texto);
  }
}
