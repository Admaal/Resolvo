package com.admaal.resolvo.infrastructure;

import com.admaal.resolvo.application.ConsultarMetricasIa;
import com.admaal.resolvo.application.RegistroLlamadas;
import com.admaal.resolvo.application.Tarifas;
import com.admaal.resolvo.domain.OrganizacionId;
import com.admaal.resolvo.domain.ResumenLlamadas;
import com.admaal.resolvo.domain.Rol;
import java.time.Instant;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AplicacionDeMetricas {

  private final RegistroLlamadas registro;
  private final Tarifas tarifas;

  public AplicacionDeMetricas(RegistroLlamadas registro, Tarifas tarifas) {
    this.registro = registro;
    this.tarifas = tarifas;
  }

  @Transactional(readOnly = true)
  public ResumenLlamadas resumen(
      OrganizacionId organizacionId, Rol rol, Instant desde, Instant hasta) {
    return new ConsultarMetricasIa(registro, tarifas).ejecutar(organizacionId, rol, desde, hasta);
  }
}
