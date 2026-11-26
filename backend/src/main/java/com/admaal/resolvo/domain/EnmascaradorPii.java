package com.admaal.resolvo.domain;

import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Sustituye datos personales del texto por marcadores antes de mandarlo a un proveedor externo.
 * Cubre emails, IBAN, tarjetas (con Luhn), DNI/NIE (con la letra de control, tambien con guion o
 * espacio) y telefonos espanoles (con espacios, guiones o puntos, y prefijo +34 o 0034). No toca
 * UUIDs, codigos de ticket ni fechas.
 */
public final class EnmascaradorPii {

  private static final Pattern EMAIL = Pattern.compile("[\\w.+-]+@[\\w-]+(?:\\.[\\w-]+)+");
  // Solo IBAN de digitos (el espanol y similares), en mayusculas o minusculas: pide al menos 16
  // digitos detras de las dos letras, asi que no se come palabras.
  private static final Pattern IBAN =
      Pattern.compile(
          "(?<!\\w)[A-Z]{2}\\d{2}(?: ?\\d{4}){4,6}(?: ?\\d{1,4})?(?!\\w)", Pattern.CASE_INSENSITIVE);
  private static final Pattern TARJETA = Pattern.compile("(?<![\\w-])\\d(?:[ -]?\\d){12,18}(?![\\w-])");
  private static final Pattern DOCUMENTO =
      Pattern.compile(
          "(?<![\\w-])(?:[XYZ]-?\\d{7}|\\d{8})[ -]?[A-Z](?![\\w-])", Pattern.CASE_INSENSITIVE);
  private static final Pattern TELEFONO =
      Pattern.compile("(?<![\\w-])(?:(?:\\+|00)34[ .-]?)?[6-9](?:[ .-]?\\d){8}(?![\\w-])");
  private static final String LETRAS_DNI = "TRWAGMYFPDXBNJZSQVHLCKE";

  private EnmascaradorPii() {}

  public static String enmascarar(String texto) {
    if (texto == null || texto.isEmpty()) {
      return texto;
    }
    String resultado = EMAIL.matcher(texto).replaceAll("[EMAIL]");
    resultado = IBAN.matcher(resultado).replaceAll("[IBAN]");
    resultado = sustituir(TARJETA, resultado, "[TARJETA]", EnmascaradorPii::pasaLuhn);
    resultado = sustituir(DOCUMENTO, resultado, "[DNI]", EnmascaradorPii::letraDeDocumentoValida);
    return TELEFONO.matcher(resultado).replaceAll("[TELEFONO]");
  }

  private static String sustituir(
      Pattern patron, String texto, String marcador, Predicate<String> valido) {
    Matcher coincidencia = patron.matcher(texto);
    StringBuilder salida = new StringBuilder();
    while (coincidencia.find()) {
      String original = coincidencia.group();
      coincidencia.appendReplacement(
          salida, Matcher.quoteReplacement(valido.test(original) ? marcador : original));
    }
    coincidencia.appendTail(salida);
    return salida.toString();
  }

  private static boolean pasaLuhn(String candidato) {
    String digitos = candidato.replaceAll("\\D", "");
    int suma = 0;
    boolean duplicar = false;
    for (int i = digitos.length() - 1; i >= 0; i--) {
      int digito = digitos.charAt(i) - '0';
      if (duplicar) {
        digito *= 2;
        if (digito > 9) {
          digito -= 9;
        }
      }
      suma += digito;
      duplicar = !duplicar;
    }
    return suma % 10 == 0;
  }

  private static boolean letraDeDocumentoValida(String documento) {
    String mayusculas = documento.toUpperCase().replaceAll("[ -]", "");
    char inicial = mayusculas.charAt(0);
    String numero = mayusculas.substring(0, mayusculas.length() - 1);
    if (inicial == 'X' || inicial == 'Y' || inicial == 'Z') {
      numero = ("XYZ".indexOf(inicial)) + numero.substring(1);
    }
    if (numero.length() != 8) {
      return false;
    }
    char letra = mayusculas.charAt(mayusculas.length() - 1);
    return LETRAS_DNI.charAt((int) (Long.parseLong(numero) % 23)) == letra;
  }
}
