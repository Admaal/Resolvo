package com.admaal.resolvo.infrastructure.seguridad;

import com.admaal.resolvo.application.EmisorDeTokens;
import com.admaal.resolvo.domain.Usuario;
import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.crypto.MACSigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import java.time.Instant;
import java.util.Date;
import javax.crypto.SecretKey;
import org.springframework.stereotype.Component;

@Component
public class EmisorNimbus implements EmisorDeTokens {

  private final SecretKey clave;

  public EmisorNimbus(SecretKey clave) {
    this.clave = clave;
  }

  @Override
  public String emitir(Usuario usuario, Instant vence) {
    JWTClaimsSet claims =
        new JWTClaimsSet.Builder()
            .subject(usuario.id().valor().toString())
            .claim("organizacionId", usuario.organizacionId().valor().toString())
            .claim("rol", usuario.rol().name())
            .expirationTime(Date.from(vence))
            .issueTime(new Date())
            .build();
    SignedJWT jwt = new SignedJWT(new JWSHeader(JWSAlgorithm.HS256), claims);
    try {
      jwt.sign(new MACSigner(clave));
    } catch (JOSEException e) {
      throw new IllegalStateException(e);
    }
    return jwt.serialize();
  }
}
