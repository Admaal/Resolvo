package com.admaal.resolvo.infrastructure.persistencia;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.admaal.resolvo.application.ComentariosDemo;
import com.admaal.resolvo.bootstrap.ResolvoApplication;
import com.admaal.resolvo.infrastructure.PostgresDePrueba;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@Tag("full")
@SpringBootTest(classes = ResolvoApplication.class)
class PurgaComentariosDemoJdbcTest {

  private static final String TICKET_SEMILLA = "44444444-4444-4444-4444-444444444442";
  private static final String AUTOR = "22222222-2222-2222-2222-222222222221";

  @DynamicPropertySource
  static void postgres(DynamicPropertyRegistry registro) {
    PostgresDePrueba.registrar(registro);
  }

  @Autowired JdbcTemplate jdbc;
  @Autowired ComentariosDemo comentarios;

  private UUID insertar(boolean semilla, Instant creadoEn) {
    UUID id = UUID.randomUUID();
    jdbc.update(
        """
        insert into comentario (id, ticket_id, autor_id, cuerpo, interno, creado_en, semilla)
        values (?, ?::uuid, ?::uuid, 'texto', false, ?, ?)
        """,
        id,
        TICKET_SEMILLA,
        AUTOR,
        java.sql.Timestamp.from(creadoEn),
        semilla);
    return id;
  }

  private boolean existe(UUID id) {
    return jdbc.queryForObject("select count(*) from comentario where id = ?", Integer.class, id) == 1;
  }

  @Test
  void borraLoDeVisitantesAntiguoYConservaLaSemillaYLoReciente() {
    Instant ahora = Instant.now();
    UUID antiguo = insertar(false, ahora.minus(30, ChronoUnit.HOURS));
    UUID reciente = insertar(false, ahora.minus(1, ChronoUnit.HOURS));
    UUID semillaAntigua = insertar(true, ahora.minus(30, ChronoUnit.DAYS));

    int borrados = comentarios.borrarCreadosAntesDe(ahora.minus(24, ChronoUnit.HOURS));

    assertThat(borrados).isGreaterThanOrEqualTo(1);
    assertThat(existe(antiguo)).isFalse();
    assertThat(existe(reciente)).isTrue();
    assertThat(existe(semillaAntigua)).isTrue();
  }

  @Test
  void laModeracionDeUnComentarioSeGuardaCompletaONoSeGuarda() {
    assertThatThrownBy(
            () ->
                jdbc.update(
                    """
                    insert into comentario (id, ticket_id, autor_id, cuerpo, interno, creado_en, oculto_motivo)
                    values (?, ?::uuid, ?::uuid, 'texto', false, now(), 'SPAM')
                    """,
                    UUID.randomUUID(),
                    TICKET_SEMILLA,
                    AUTOR))
        .isInstanceOf(DataIntegrityViolationException.class);
  }

  @Test
  void laMigracionMarcaLosCuatroComentariosDeLaSemilla() {
    Integer marcados =
        jdbc.queryForObject(
            "select count(*) from comentario where id::text like '55555555-%' and semilla",
            Integer.class);

    assertThat(marcados).isEqualTo(4);
  }
}
