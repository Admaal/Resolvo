package com.admaal.resolvo.infrastructure.persistencia;

import com.admaal.resolvo.application.Reloj;
import com.admaal.resolvo.domain.Limite;
import com.admaal.resolvo.infrastructure.FabricaDeLimites;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Limites de uso guardados en Postgres: cada peticion contada es una fila de {@code limite_marca}.
 * Sobreviven a un reinicio y valen con varias instancias. Un cerrojo de transaccion por (ambito,
 * clave) hace que dos peticiones simultaneas no pasen juntas por el ultimo hueco. La clave se
 * guarda como SHA-256: el login la forma con el email y la IP.
 */
@Component
public class LimitesJdbc implements FabricaDeLimites {

  /** Ninguna ventana configurada pasa de un dia: lo mas viejo ya no cuenta para nadie. */
  private static final Duration CONSERVAR = Duration.ofDays(1);

  private final JdbcTemplate jdbc;
  private final TransactionTemplate transacciones;
  private final Reloj reloj;

  @Autowired
  public LimitesJdbc(JdbcTemplate jdbc, TransactionTemplate transacciones, Reloj reloj) {
    this.jdbc = jdbc;
    this.transacciones = transacciones;
    this.reloj = reloj;
  }

  LimitesJdbc(JdbcTemplate jdbc, TransactionTemplate transacciones) {
    this(jdbc, transacciones, Instant::now);
  }

  @Override
  public Limite crear(String ambito, int maximo, Duration ventana) {
    if (maximo < 1 || ventana.isZero() || ventana.isNegative()) {
      throw new IllegalArgumentException("maximo y ventana deben ser positivos");
    }
    return new LimiteEnBase(ambito, maximo, ventana);
  }

  /** Borra las marcas de hace mas de un dia, de todas las claves. */
  public void purgar(Instant ahora) {
    jdbc.update("delete from limite_marca where en < ?", Timestamp.from(ahora.minus(CONSERVAR)));
  }

  @Scheduled(fixedDelay = 3_600_000, initialDelay = 300_000)
  void purgarCadaHora() {
    purgar(reloj.ahora());
  }

  static String resumen(String clave) {
    try {
      byte[] hash =
          MessageDigest.getInstance("SHA-256").digest(clave.getBytes(StandardCharsets.UTF_8));
      return HexFormat.of().formatHex(hash);
    } catch (NoSuchAlgorithmException imposible) {
      throw new IllegalStateException(imposible);
    }
  }

  private final class LimiteEnBase implements Limite {

    private final String ambito;
    private final int maximo;
    private final Duration ventana;

    LimiteEnBase(String ambito, int maximo, Duration ventana) {
      this.ambito = ambito;
      this.maximo = maximo;
      this.ventana = ventana;
    }

    @Override
    public Decision admitir(String clave, Instant ahora) {
      String resumen = resumen(clave);
      return transacciones.execute(
          estado -> {
            jdbc.queryForList(
                "select pg_advisory_xact_lock(hashtextextended(? || '|' || ?, 0))",
                ambito,
                resumen);
            jdbc.update(
                "delete from limite_marca where ambito = ? and clave = ? and en <= ?",
                ambito,
                resumen,
                Timestamp.from(ahora.minus(ventana)));
            Timestamp primera =
                jdbc.queryForObject(
                    "select min(en) from limite_marca where ambito = ? and clave = ?",
                    Timestamp.class,
                    ambito,
                    resumen);
            Integer contadas =
                jdbc.queryForObject(
                    "select count(*) from limite_marca where ambito = ? and clave = ?",
                    Integer.class,
                    ambito,
                    resumen);
            if (contadas != null && contadas >= maximo) {
              return new Decision(false, ventana.minus(Duration.between(primera.toInstant(), ahora)));
            }
            jdbc.update(
                "insert into limite_marca (ambito, clave, en) values (?, ?, ?)",
                ambito,
                resumen,
                Timestamp.from(ahora));
            return new Decision(true, Duration.ZERO);
          });
    }

    @Override
    public void reiniciar(String clave) {
      jdbc.update(
          "delete from limite_marca where ambito = ? and clave = ?", ambito, resumen(clave));
    }
  }
}
