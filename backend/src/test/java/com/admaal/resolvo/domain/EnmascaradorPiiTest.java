package com.admaal.resolvo.domain;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class EnmascaradorPiiTest {

  @Test
  void tapaUnEmail() {
    assertThat(EnmascaradorPii.enmascarar("Escribid a ana.lopez+x@mail.example.com ya"))
        .isEqualTo("Escribid a [EMAIL] ya");
  }

  @Test
  void tapaTelefonosEspanolesConYSinFormato() {
    assertThat(EnmascaradorPii.enmascarar("Llamad al 612345678.")).isEqualTo("Llamad al [TELEFONO].");
    assertThat(EnmascaradorPii.enmascarar("Tel +34 612 34 56 78 y 91-234-56-78"))
        .isEqualTo("Tel [TELEFONO] y [TELEFONO]");
  }

  @Test
  void tapaUnDniConLetraValida() {
    assertThat(EnmascaradorPii.enmascarar("DNI 12345678Z")).isEqualTo("DNI [DNI]");
  }

  @Test
  void tapaUnNieConLetraValida() {
    assertThat(EnmascaradorPii.enmascarar("NIE X1234567L")).isEqualTo("NIE [DNI]");
  }

  @Test
  void unDniConLetraIncorrectaNoSeTapa() {
    assertThat(EnmascaradorPii.enmascarar("Ref 12345678A")).isEqualTo("Ref 12345678A");
  }

  @Test
  void tapaUnIbanConYSinEspacios() {
    assertThat(EnmascaradorPii.enmascarar("IBAN ES9121000418450200051332 fin")).isEqualTo("IBAN [IBAN] fin");
    assertThat(EnmascaradorPii.enmascarar("IBAN ES91 2100 0418 4502 0005 1332 fin"))
        .isEqualTo("IBAN [IBAN] fin");
  }

  @Test
  void tapaUnaTarjetaQuePasaLuhn() {
    assertThat(EnmascaradorPii.enmascarar("Tarjeta 4111 1111 1111 1111 caducada"))
        .isEqualTo("Tarjeta [TARJETA] caducada");
    assertThat(EnmascaradorPii.enmascarar("4111-1111-1111-1111")).isEqualTo("[TARJETA]");
  }

  @Test
  void unaTarjetaQueNoPasaLuhnNoSeTapa() {
    assertThat(EnmascaradorPii.enmascarar("Pedido 4111 1111 1111 1112")).isEqualTo("Pedido 4111 1111 1111 1112");
  }

  @Test
  void noTocaUuidsNiCodigosDeTicketNiFechas() {
    String texto =
        "Ticket RS-12 id 11111111-1111-1111-1111-111111111111 del 2026-11-19 con 3 adjuntos";
    assertThat(EnmascaradorPii.enmascarar(texto)).isEqualTo(texto);
  }

  @Test
  void textoNuloOVacioSeDevuelveTalCual() {
    assertThat(EnmascaradorPii.enmascarar("")).isEmpty();
    assertThat(EnmascaradorPii.enmascarar(null)).isNull();
  }

  @Test
  void tapaVariosDatosEnUnMismoTexto() {
    assertThat(EnmascaradorPii.enmascarar("a@b.es, 612345678 y 12345678Z"))
        .isEqualTo("[EMAIL], [TELEFONO] y [DNI]");
  }

  @Test
  void tapaTarjetasValidasDeVariasMarcasYLongitudes() {
    assertThat(EnmascaradorPii.enmascarar("5500 0000 0000 0004")).isEqualTo("[TARJETA]");
    assertThat(EnmascaradorPii.enmascarar("378282246310005")).isEqualTo("[TARJETA]");
    assertThat(EnmascaradorPii.enmascarar("6011111111111117")).isEqualTo("[TARJETA]");
    assertThat(EnmascaradorPii.enmascarar("4012888888881881")).isEqualTo("[TARJETA]");
  }

  @Test
  void noTapaNumerosLargosQueNoPasanLuhn() {
    assertThat(EnmascaradorPii.enmascarar("5500 0000 0000 0005")).isEqualTo("5500 0000 0000 0005");
    assertThat(EnmascaradorPii.enmascarar("378282246310006")).isEqualTo("378282246310006");
    assertThat(EnmascaradorPii.enmascarar("4012888888881882")).isEqualTo("4012888888881882");
  }

  @Test
  void unaLetraSinOchoDigitosNoEsUnDni() {
    assertThat(EnmascaradorPii.enmascarar("Ref 1234567L")).isEqualTo("Ref 1234567L");
  }

  @Test
  void tapaDniYNieEnMinusculasConLetraValida() {
    assertThat(EnmascaradorPii.enmascarar("dni 12345678z y x1234567l")).isEqualTo("dni [DNI] y [DNI]");
  }

  @Test
  void tapaNieConLasTresInicialesValidas() {
    assertThat(EnmascaradorPii.enmascarar("Y1234567X")).isEqualTo("[DNI]");
    assertThat(EnmascaradorPii.enmascarar("Z1234567R")).isEqualTo("[DNI]");
  }
}
