package com.admaal.resolvo.domain;

/** Lo gastado de IA en un periodo: llamadas hechas y tokens (entrada mas salida). */
public record Consumo(int llamadas, long tokens) {}
