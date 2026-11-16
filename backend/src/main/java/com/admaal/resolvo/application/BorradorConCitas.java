package com.admaal.resolvo.application;

import com.admaal.resolvo.domain.FragmentoId;
import java.util.List;

public record BorradorConCitas(String texto, List<FragmentoId> citas) {}
