package com.admaal.resolvo.infrastructure.persistencia;

import com.admaal.resolvo.application.SesionesRefresh;
import com.admaal.resolvo.domain.BonoRefresh;
import com.admaal.resolvo.domain.UsuarioId;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
public class SesionesRefreshJpa implements SesionesRefresh {

  private final BonoJpa bonos;
  private final SecureRandom aleatorio = new SecureRandom();

  public SesionesRefreshJpa(BonoJpa bonos) {
    this.bonos = bonos;
  }

  @Override
  public void guardar(BonoRefresh bono) {
    BonoEntity fila = bonos.findById(hash(bono.token())).orElseGet(BonoEntity::new);
    fila.setTokenHash(hash(bono.token()));
    fila.setUsuarioId(bono.usuarioId().valor());
    fila.setFamilia(bono.familia());
    fila.setConsumido(bono.consumido());
    fila.setRevocado(bono.revocado());
    bonos.save(fila);
  }

  @Override
  public Optional<BonoRefresh> porToken(String token) {
    return bonos
        .findById(hash(token))
        .map(
            fila ->
                new BonoRefresh(
                    token,
                    new UsuarioId(fila.getUsuarioId()),
                    fila.getFamilia(),
                    fila.isConsumido(),
                    fila.isRevocado()));
  }

  @Override
  public void revocarFamilia(UUID familia) {
    bonos.revocarFamilia(familia);
  }

  @Override
  public String tokenNuevo() {
    byte[] bytes = new byte[32];
    aleatorio.nextBytes(bytes);
    return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
  }

  static String hash(String token) {
    try {
      return HexFormat.of()
          .formatHex(MessageDigest.getInstance("SHA-256").digest(token.getBytes(StandardCharsets.UTF_8)));
    } catch (NoSuchAlgorithmException e) {
      throw new IllegalStateException(e);
    }
  }
}
