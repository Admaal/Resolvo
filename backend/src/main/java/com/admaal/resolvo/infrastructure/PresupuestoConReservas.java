package com.admaal.resolvo.infrastructure;

import com.admaal.resolvo.application.Presupuesto;
import com.admaal.resolvo.application.PresupuestoAgotado;
import com.admaal.resolvo.application.RegistroLlamadas;
import com.admaal.resolvo.application.Reloj;
import com.admaal.resolvo.domain.Consumo;
import com.admaal.resolvo.domain.OrganizacionId;
import com.admaal.resolvo.domain.PresupuestoDiario;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Presupuesto diario por organizacion (dia UTC). Suma lo ya registrado en {@code llamada_ia} y las
 * reservas abiertas en memoria, de modo que dos peticiones simultaneas no pasan juntas por la
 * ultima llamada libre. Vale para una sola instancia de la API.
 */
@Component
public class PresupuestoConReservas implements Presupuesto {

  private final RegistroLlamadas registro;
  private final Reloj reloj;
  private final PresupuestoDiario limite;
  private final Map<OrganizacionId, Integer> enCurso = new HashMap<>();

  @Autowired
  public PresupuestoConReservas(
      RegistroLlamadas registro,
      Reloj reloj,
      @Value("${resolvo.ia.presupuesto-diario.llamadas:200}") int llamadas,
      @Value("${resolvo.ia.presupuesto-diario.tokens:300000}") long tokens) {
    this(registro, reloj, new PresupuestoDiario(llamadas, tokens));
  }

  public PresupuestoConReservas(RegistroLlamadas registro, Reloj reloj, PresupuestoDiario limite) {
    this.registro = registro;
    this.reloj = reloj;
    this.limite = limite;
  }

  @Override
  public synchronized Reserva reservar(OrganizacionId organizacionId) {
    Instant desde = reloj.ahora().truncatedTo(ChronoUnit.DAYS);
    Instant hasta = LocalDate.ofInstant(desde, ZoneOffset.UTC).plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();
    Consumo consumo = registro.consumoDe(organizacionId, desde, hasta);
    if (!limite.admite(consumo, enCurso.getOrDefault(organizacionId, 0))) {
      throw new PresupuestoAgotado();
    }
    enCurso.merge(organizacionId, 1, Integer::sum);
    AtomicBoolean cerrada = new AtomicBoolean();
    return () -> {
      if (cerrada.compareAndSet(false, true)) {
        liberar(organizacionId);
      }
    };
  }

  private synchronized void liberar(OrganizacionId organizacionId) {
    enCurso.computeIfPresent(organizacionId, (org, abiertas) -> abiertas <= 1 ? null : abiertas - 1);
  }
}
