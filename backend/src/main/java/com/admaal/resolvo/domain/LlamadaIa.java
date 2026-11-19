package com.admaal.resolvo.domain;

import java.time.Instant;
import java.util.UUID;

/**
 * Una llamada al modelo de lenguaje: de que operacion, con que modelo, cuanto costo en tokens y
 * cuanto tardo. Las fallidas llevan tokens a cero y la duracion que se espero. La organizacion es
 * nula si la llamada no es de nadie.
 */
public record LlamadaIa(
    UUID id,
    OrganizacionId organizacionId,
    String operacion,
    String modelo,
    int tokensEntrada,
    int tokensSalida,
    long milisegundos,
    boolean exito,
    Instant ocurridaEn) {}
