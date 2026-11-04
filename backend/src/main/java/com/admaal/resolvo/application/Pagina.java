package com.admaal.resolvo.application;

import java.util.List;

public record Pagina<T>(List<T> elementos, int pagina, int tamano, long total) {}
