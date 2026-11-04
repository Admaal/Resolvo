package com.admaal.resolvo.application;

import com.admaal.resolvo.domain.Usuario;
import java.time.Instant;

public interface EmisorDeTokens {
  String emitir(Usuario usuario, Instant vence);
}
