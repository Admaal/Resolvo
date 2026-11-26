package com.admaal.resolvo.infrastructure.web;

import com.admaal.resolvo.application.Reloj;
import com.admaal.resolvo.domain.Limite;
import com.admaal.resolvo.infrastructure.FabricaDeLimites;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/** Limita por usuario las peticiones que llaman al modelo o a los embeddings. */
@Component
public class LimiteDeIa implements HandlerInterceptor {

  private final Limite limite;
  private final Reloj reloj;

  public LimiteDeIa(
      Reloj reloj,
      FabricaDeLimites limites,
      @Value("${resolvo.limites.ia-por-minuto:10}") int porMinuto) {
    this.reloj = reloj;
    this.limite = limites.crear("ia", porMinuto, Duration.ofMinutes(1));
  }

  @Override
  public boolean preHandle(
      HttpServletRequest peticion, HttpServletResponse respuesta, Object manejador) {
    var decision = limite.admitir(Actor.actual().id().valor().toString(), reloj.ahora());
    if (!decision.admitida()) {
      throw new DemasiadasPeticiones("demasiadas-peticiones", decision.reintentarEn());
    }
    return true;
  }
}
