# Resolvo

## Qué es

Un helpdesk B2B multi-tenant para equipos de soporte: cada organización gestiona sus tickets, su equipo y sus SLA sin ver los de nadie más. Más adelante, un asistente IA clasifica los tickets y redacta respuestas con fuentes citadas, siempre aprobadas por una persona.

## Para quién

Portfolio personal. Nadie real lo usa: la demo pública corre con datos ficticios y tres usuarios de demostración (CLIENTE, AGENTE, ADMIN). No hay clientes ni datos de terceros.

## Restricciones

- Plataforma: web. API HTTP y SPA; sin app móvil.
- Datos: nube, solo ficticios. Nunca datos de personas reales.
- Dinero o recursos limitados: sí. Coste objetivo 0 € y ninguna tarjeta asociada.
- Hosting: Neon Free (Postgres + pgvector), Render Free (API Spring Boot en Docker, un único ping para no pasar de 750 h/mes) y Cloudflare Pages (Angular). Ver [ADR-0009](adr/0009-hosting.md).
- Tiempo: unas 8–10 h/semana. El calendario de 24 semanas del plan técnico es un mapa, no un compromiso: si el tiempo baja, se estira la fase 6, no se recortan tests.

## Fase 1 (en detalle)

Al terminar la fase 1, sin IA y sin URL pública, el usuario puede:

- Entrar con email y contraseña y recibir un access token corto; el refresh vive en cookie HttpOnly.
- Crear un ticket como CLIENTE y verlo nacer en estado ABIERTO con su vencimiento de SLA calculado por categoría y prioridad.
- Pedir un recurso de otra organización y recibir 404, sin filtrar que existe.
- Pedir una transición de estado no permitida y recibir 409 sin que el ticket cambie.
- Intentar asignar un ticket como CLIENTE y recibir 403.
- Comentar un ticket; los comentarios internos no aparecen a los usuarios CLIENTE.
- Listar tickets paginados y filtrar por estado, prioridad y asignado.

Fuera de la fase 1:

- Cualquier función de IA (clasificación, sugerencias, RAG, citas).
- El despliegue público y el aviso de «despertando servidor…».
- CRUD completo de administración (organizaciones, equipos, categorías) más allá de la semilla.
- Eventos, observabilidad y evals en CI.

## Fases siguientes

- Fase 2: MVP desplegado, URL pública con login demo y datos semilla idempotentes.
- Fase 3: RAG con pgvector, sugerencias con citas y aprobación humana.
- Fase 4: clasificación de tickets (categoría y prioridad sugeridas) y evals en CI con umbral de calidad, coste y latencia.
- Fase 5: outbox, eventos idempotentes y métricas de tokens, coste y latencia.
- Fase 6: seguridad (prompt injection, PII, rate limit, presupuesto diario) y difusión.

## Decisiones técnicas

- [ADR-0001](adr/0001-lenguaje-framework.md): Java 21 + Spring Boot 3.5.x.
- [ADR-0002](adr/0002-arquitectura.md): monolito modular hexagonal con ArchUnit.
- [ADR-0003](adr/0003-datos-migraciones.md): Postgres + pgvector, Flyway desde V1 y `ddl-auto=validate`.
- [ADR-0004](adr/0004-frontend.md): Angular standalone con controles nativos y estilos propios (Material se retiró el 2026-11-26). El diseño visual de la web sale de [design.md](design.md).
- [ADR-0005](adr/0005-auth.md): JWT propio, access corto en memoria y refresh en cookie HttpOnly.
- [ADR-0006](adr/0006-llm.md): Spring AI detrás de puertos; solo existe el perfil `gemini` (enmienda del 2026-11-25).
- [ADR-0007](adr/0007-embeddings.md): un modelo y una dimensión fijos; cambiarlos exige migración y reindexado.
- [ADR-0008](adr/0008-eventos.md): outbox transaccional, cola en Postgres online y Redpanda en local.
- [ADR-0009](adr/0009-hosting.md): Neon Free, Render Free y Cloudflare Pages.
- [ADR-0010](adr/0010-tests-y-valla.md): JUnit 5, Testcontainers, ArchUnit y PIT en dominio al 80 %; el end to end con Playwright no se montó (enmienda del 2026-11-25).
- [ADR-0011](adr/0011-evals.md): evals de IA contra Gemini real, en un job aparte y fuera del gate obligatorio.
- [ADR-0012](adr/0012-metricas-ia.md): métricas de la IA en Postgres, endpoint para ADMIN y coste a precio de lista con tarifas citadas.
- [ADR-0013](adr/0013-seguridad-ia.md): seguridad de la IA (inyección, PII), límites de uso y presupuesto diario por organización.

## Preguntas abiertas

- Versión exacta de Spring Boot 3.5.x y de Spring AI: Spring Boot queda fijado en 3.5.16 al crear el esqueleto; la versión de Spring AI se fija al añadir el primer adapter de LLM.
- Cuotas reales de Gemini y del free tier de Neon y Render: se anotan con fecha antes de la fase 2.
- Latencia de las llamadas a Gemini: el p95 de los evals oscila entre 8,6 y 12,7 s; el umbral se relajó de 8 a 15 s (ADR-0011). Falta bajarla de verdad si molesta en la demo.

## Demostración local

Organización Acme. La contraseña ficticia de los tres usuarios es `demo-resolvo`:

- `cliente@acme.test` (CLIENTE)
- `agente@acme.test` (AGENTE)
- `admin@acme.test` (ADMIN)

Categorías de semilla, sin CRUD: Incidencia (1 h / 4 h / 8 h / 24 h) y Consulta (2 h / 8 h / 24 h / 48 h), en ese orden de prioridad. El vencimiento es de primera respuesta. La segunda organización solo existe en los tests.

## Preguntas cerradas en la fase 1

- Angular 22.2.5, la estable del CLI el 2026-11-04. No se sube de mayor a mitad del plan.
- JWT firmado con Nimbus y validado con `spring-boot-starter-oauth2-resource-server`. Sin jjwt.
- Tests de la SPA con Vitest, el runner por defecto del CLI desde la versión 21.

## Preguntas cerradas en la fase 2

- Cookie de refresh entre Pages y Render: proxy de Pages en el mismo origen, sin CORS y con `SameSite=Lax` (ADR-0005).
- `API_URL` en build: no hace falta, la SPA llama a `/api` (ADR-0004).
- Arranque de la JVM: CDS en la imagen, sin AOT (ADR-0009).
- Ping: GitHub Actions cada 5 minutos en vez de un cron externo, para no crear otra cuenta.

## Preguntas cerradas en la fase 3

- Embeddings: `gemini-embedding-001` con N = 768 (ADR-0007).
- Fuentes del RAG: artículos ficticios de ayuda en semilla y tickets RESUELTO o CERRADO de la misma organización. Sin CRUD de artículos.
- La clasificación de tickets pasa a la fase 4.

## Preguntas cerradas en la fase 4

- La clasificación se pide bajo demanda (AGENTE o ADMIN), no al abrir el ticket: la demo es pública y cada clasificación gasta cuota. Una pendiente por ticket; la persona puede corregirla antes de aplicarla y aplicar recalcula el SLA.
- Los evals miden calidad, coste en tokens y latencia contra Gemini real, en un job que no bloquea el merge (ADR-0011).
- El umbral de similitud del RAG existe pero queda desactivado (2.0) hasta calibrarlo con la primera corrida real de los evals.

## Preguntas cerradas en la fase 5

- Eventos: solo el adapter Postgres del outbox; Redpanda queda fuera (ADR-0008, enmendada). Dos eventos, los que tienen consumidor: ticket resuelto y ticket reabierto.
- Sin poller continuo: Neon se suspende y una consulta periódica lo mantendría despierto. El procesador se despierta al publicar, al arrancar y, si hubo fallos, cada minuto.
- Métricas: una fila por llamada a la IA y un endpoint de resumen solo para ADMIN, sin pantalla en la SPA (ADR-0012).
- Coste: USD a precio de lista, con tarifas citadas; sin tarifa, el coste queda desconocido y no cero. No se convierte a euros.
