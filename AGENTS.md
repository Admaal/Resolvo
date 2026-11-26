# Resolvo

Helpdesk B2B multi-tenant para equipos de soporte, en solitario y como portfolio. El producto está en [docs/producto.md](docs/producto.md) y las decisiones técnicas en [docs/adr/](docs/adr/).

Este archivo es la fuente de verdad de la fase, de la valla y de los riesgos abiertos. El historial resumido vive en [docs/fases.md](docs/fases.md) y el detalle en [docs/bitacora.md](docs/bitacora.md).

## Fase actual

**Fases 0 a 6 hechas y desplegadas** (la 6 el 2026-11-19), con pulido posterior hasta el 2026-11-25: rediseño de la SPA, moderación de comentarios, código del ticket en la URL, sesión renovada al caducar el token, visibilidad por autor para el CLIENTE y la auditoría de portfolio. **No hay fase en curso.** Resumen por fase en [docs/fases.md](docs/fases.md); el detalle (qué se comprobó en la demo, RED, verificadores, cifras de cada gate) en [docs/bitacora.md](docs/bitacora.md).

La demo corre en https://resolvo-6lc.pages.dev: SPA en Cloudflare Pages con una Pages Function que hace de proxy `/api` a la API en Render Free (Docker, Frankfurt) y Postgres con pgvector en Neon Free. Migraciones V1 a V11 aplicadas en Neon (V10, sin columnas de bloqueo, y V11, límites de uso, el 2026-11-26 a las 12:08 UTC). Ping cada 5 minutos con GitHub Actions.

Lo que hay que saber antes de tocar nada:

- La IA es un perfil: sin `gemini` no se carga Spring AI y los endpoints de IA responden 503 (ADR-0006). Embeddings `gemini-embedding-001` a 768 (ADR-0007).
- Los evals (`backend\gradlew.bat -p backend evals`, workflow [evals.yml](.github/workflows/evals.yml)) llaman a Gemini real y **no forman parte** de `Gate obligatorio` (ADR-0011). Necesitan `RESOLVO_GEMINI_API_KEY`; en local no suele estar, se lanzan con `gh workflow run evals.yml --ref <rama>`.
- Eventos con outbox en Postgres e idempotencia por consumidor (ADR-0008); métricas de la IA en `GET /api/metricas/ia`, solo ADMIN (ADR-0012).
- Seguridad de la IA, límites de uso y presupuesto diario por organización en ADR-0013. La IP real del cliente llega en `X-Cliente-Ip`, firmada con `PROXY_CLAVE` (Pages) = `RESOLVO_PROXY_CLAVE` (Render).
- Un CLIENTE solo ve y comenta sus tickets (`Ticket.visiblePara`); sobre un ticket ajeno todo responde 404 con el mismo cuerpo que uno inexistente, y el rol se evalúa después.

## Stack

- Lenguaje y framework: Java 21 LTS con Spring Boot 3.5.16 (ADR-0001). Gradle wrapper 9.3.1 en `backend/`, un único módulo, paquete base `com.admaal.resolvo`.
- Tests: `backend\gradlew.bat -p backend test` (JUnit 5 + AssertJ + ArchUnit). El tag `full` marca lo que arranca Spring o Docker y queda fuera del bucle rápido.
- Dominio: `backend/src/main/java/com/admaal/resolvo/domain/` (sin Spring, sin JPA, sin Spring AI).
- Casos de uso: `backend/src/main/java/com/admaal/resolvo/application/` (solo depende de `domain` y de sus puertos).
- Adapters: `backend/src/main/java/com/admaal/resolvo/infrastructure/` (JPA, JDBC y pgvector, Flyway, JWT, HTTP y `ai/` con Spring AI).
- Lint de pureza: ArchUnit, en `backend/src/test/java/com/admaal/resolvo/architecture/`.
- Frontend: Angular 22.2.5 standalone con controles nativos y estilos propios (sin Angular Material ni CDK desde el 2026-11-26) y Vitest, en `frontend/`. El access token vive en un campo de `AuthService`. El panel de sugerencias (`features/tickets/sugerencia-panel.ts`) solo se pinta a AGENTE y ADMIN.
- Guía web: el repo declara el plugin `modern-web-guidance@googlechrome` (skills de Chrome para agentes; `.claude/settings.json`). This project's Baseline target is Baseline Widely available. Antes de tocar CSS, HTML o formularios del frontend, consultar `npx modern-web-guidance@latest search "<tema>"` y `retrieve <id>`; `legacy-scan frontend/src` busca patrones antiguos. Lo visual lo decide [docs/design.md](docs/design.md); los tokens viven en `frontend/src/styles/` (`tokens.css`, `semantic.css`, `themes.css`).

## Principios no negociables

Cada principio tiene una comprobación mecánica. Un principio sin comprobación no es una regla.

- El dominio no importa Spring, JPA, Hibernate ni Spring AI, ni depende de `application`, `infrastructure` o `bootstrap`. Comprobación: `DomainPurityTest.elDominioNoDependeDeSpringJpaNiSpringAi` (ADR-0002).
- `application` no importa `infrastructure`, `bootstrap`, Spring ni JPA. Comprobación: `ApplicationBoundaryTest.laAplicacionSoloDependeDelDominio` (ADR-0002).
- Toda clase nueva de `domain` o `application` nace con test, y PIT la cubre al 80 %. Comprobación: gate `mutation` en `scripts/gates.ps1 full`.
- Ningún test se debilita para pasar: ni `skip`, ni assertions fuera, ni umbrales rebajados.

## Valla

1. **Pureza de dominio:** ArchUnit en los dos tests de arriba. Falla en el gate, no en la review.
2. **TDD:** un test en rojo del caso de uso antes de implementarlo. `gates.ps1 fast` anota la línea `RED` en `.cursor/red.log` (versionado, `merge=union`). `gate-timings.log` sigue ignorado.
3. **Mutation testing** solo de `domain` y `application`, umbral 80 %, `failWhenNoMutations` activo. Comando: `backend\gradlew.bat -p backend pitest`. El gate `mutation` solo corre si el diff toca esas rutas.
4. **Hooks** (PowerShell, `$ErrorActionPreference = "Continue"`):
   - Los hooks existen en dos formatos, `.cursor/hooks.json` y `.claude/settings.json`, y llaman a los mismos scripts de `.cursor/hooks/` (`-Cliente claude` para Claude Code).
   - `postToolUse` (matcher `Write`): `scripts/gates.ps1 fast <ruta>`, que elige los tests por la ruta escrita. Techo del hook: 30 s.
   - `stop`: no hace nada si no hay cambios. Si los hay, `scripts/gates.ps1 full` con candado para no solapar dos gauntlets. Techo: 300 s. `loop_limit`: 3.
   - `beforeShellExecution`: `guard-shell.ps1` bloquea `git reset --hard`, `git checkout -- <ruta>`, `git clean -fd`, `rm -rf` y `Remove-Item -Recurse` dentro del repo, y el push directo a `main` cuando exista `.cursor/proteger-main`.
   - `scripts/gates.ps1` toma un candado propio para que dos escrituras seguidas no peleen por `build/test-results`.
   - Duración de cada gate en `.cursor/hooks/gate-timings.log`.
5. **CI** ([.github/workflows/ci.yml](.github/workflows/ci.yml)): llama a `scripts/gates.ps1 full`, con caché de Gradle y el job agregador bloqueante `Gate obligatorio`. Misma fuente de comandos que los hooks.
6. **Rama `main`:** el repo es público (`github.com/Admaal/Resolvo`) y tiene un ruleset activo desde el 2026-11-15 (Settings, Rules, `main`): exige el check `Gate obligatorio` y prohíbe force push y borrado. Sin lista de bypass: los cambios llegan por rama y PR, y un push directo a `main` se rechaza. Si el repo pasara a privado con cuenta gratuita, se usaría el fichero `.cursor/proteger-main`.

### Presupuestos de feedback

Medidos el 2026-11-04 sobre Windows, con dominio y casos de uso ya en el classpath. La ruta de arquitectura ejecuta lint (`classes testClasses`) y luego los tests de arquitectura. `scripts/medir-bucle.ps1` no terminó: el daemon de Gradle del entorno se quedaba sin cliente. Las cifras son de esos dos comandos, medidos en serie, que es lo que hace el gate.

- Bucle rápido (caliente, 5 corridas, suma lint + arquitectura): 1703-1769 ms. p95 **1769 ms**.
- Bucle rápido (frío, una corrida tras `gradlew --stop`): lint 4560 ms + arquitectura 2599 ms = **7159 ms**.
- Completo sin mutación (caliente, una corrida de `test`): **3908 ms**.
- Mutación: primera pasada 6 s, 121/124 mutantes (98 %). El gate `full` en caliente, el 2026-11-04, tardó **13 s** (tests en caché más la mutación). Muy por debajo del techo de 300 s.
- Frontend (`ng test`, Vitest, una pasada): unos **5 s** en el hook. Va aparte del presupuesto Java.

**Presupuesto del bucle rápido: 1,8 s en caliente** para la ruta de arquitectura. Supera el 1,3 s de la fase 0 porque el gate compila y luego testa, y el classpath ya tiene dominio. Los techos de `hooks.json` (30 s y 300 s) siguen holgados. El prefijo `infrastructure/` del gate rápido sigue en `-Pfast` para que Docker no entre en el hook de 30 s. El prefijo `frontend/` lanza `npm test`.

Repetir la medición: `powershell -NoProfile -File scripts/medir-bucle.ps1`.

## Comandos

- Rápido: `powershell -NoProfile -File scripts/gates.ps1 fast backend/src/main/java/com/admaal/resolvo/domain/Ticket.java`
- Completo: `powershell -NoProfile -File scripts/gates.ps1 full`
- Tests del backend desde la raíz: `backend\gradlew.bat -p backend test` (Windows) o `bash backend/gradlew -p backend test`
- Mutación desde la raíz: `backend\gradlew.bat -p backend pitest`
- SPA: `cd frontend && npm start` (proxy a `http://localhost:8080`). La API local necesita `RESOLVO_JWT_SECRET` de al menos 32 bytes y Postgres.

## Riesgos abiertos

Solo lo que sigue vigente. La historia de cada uno, con fechas, runs y cifras, está en [docs/bitacora.md](docs/bitacora.md); al cerrar un trabajo se añade allí su entrada y aquí solo lo que quede abierto.

**Secretos por rotar** (se compartieron en chats): la contraseña de `neondb_owner` (2026-11-14; actualizar `RESOLVO_DB_PASSWORD` y `RESOLVO_DB_MIGRATION_PASSWORD` en Render), la clave de Gemini de diagnóstico (2026-11-16; revocarla en AI Studio y confirmar que la de Render es otra) y `PROXY_CLAVE`/`RESOLVO_PROXY_CLAVE` (2026-11-19; `openssl rand -hex 32`, mismo valor en Render y con `npx wrangler pages secret put PROXY_CLAVE --project-name resolvo`).

**Despliegue y operación**
- `gh` 2.104 está instalado en `C:\Program Files\GitHub CLI` y con sesión como `Admaal`: los PRs, su CI y el merge se hacen con él (`gh pr create`, `gh pr merge --merge`).
- **Cloudflare Pages no está conectado a Git**: un merge a `main` despliega la API en Render, y la SPA la sube el workflow [despliegue.yml](.github/workflows/despliegue.yml) cuando cambia `frontend/` (secretos `CLOUDFLARE_API_TOKEN` y `CLOUDFLARE_ACCOUNT_ID`; sin ellos termina en verde sin desplegar). A mano, desde `frontend/`: `npm run build && npx wrangler pages deploy --project-name resolvo --branch main` (sin `--branch main` sale como preview). Necesita `wrangler login`.
- Arranque en Render con Spring AI de unos 35-40 s (3 s en local), medido el 2026-11-26 con `Started ResolvoApplication in` en seis arranques: entre 34,7 y 39,6 s. Antes del PR #49 iba de 51 a 61 s, porque el archivo CDS no se usaba. A eso se suman unos 2,5 s de JVM y lo que tarde Render en despertar el contenedor. Lo cubre el aviso de la SPA.
- Los límites de uso (IA, login, comentarios) viven en Postgres desde el PR #52 (`limite_marca`, V11, con cerrojo consultivo por clave): sobreviven a un reinicio y valen con varias instancias. Desde el PR #59 las reservas en curso del presupuesto diario también viven en Postgres (`presupuesto_reserva`, V12, cerrojo por organización, caducan a los 5 min si nadie las cierra). `RESOLVO_LIMITE_COMENTARIOS_POR_MINUTO=10` está puesta explícitamente en Render.
- Tras despliegues seguidos se han visto 502 de `/api/salud` y 500 de un chunk de JS en Pages, pasajeros. Los logs de Render no tienen ningún 5xx desde el 2026-11-20: el 502 sale del borde de Render durante el cambio de instancia, porque el servicio no tiene health check (`healthCheckPath` vacío). **Pendiente a mano**: poner `/api/salud` en Render, Settings, Health Checks (el MCP no puede cambiarlo). El 500 del chunk no se reproduce; la hipótesis es un HTML viejo pidiendo un chunk del despliegue nuevo.
- El procesador de eventos solo corre dentro de la API: si Render se duerme con eventos pendientes, esperan al siguiente arranque. Desde el PR #60 una fila `FALLIDO` de `evento_outbox` se reactiva cada hora, hasta tres rondas (`rondas`, V13); después se queda `FALLIDO`. Revertir el código con eventos `COMENTARIO_MODERADO` pendientes rompería la lectura por tipo desconocido.
- Cuotas a 2026-11-26: Neon free_v3 (periodo del 11-02 al 12-01) lleva unas 62,8 CU-h de cómputo y unas 250 h activas (el ping de 5 min lo mantiene despierto), con 8,9 MB de base sobre el límite de 1 GiB por rama. Render Free, una instancia. La cuota de Gemini es la del nivel gratuito del proyecto `Proptech` de AI Studio y se comparte si ese proyecto se usa para otra cosa.

**IA**
- Latencia: el p95 de los evals varía entre 1,5 y 12,7 s según el modelo que elija el sondeo; los tres runs del 2026-11-26 dieron entre 5,5 y 9,5 s. El umbral está en 15 s (ADR-0011). El nivel de razonamiento es configurable desde el PR #55 (`RESOLVO_GEMINI_RAZONAMIENTO`), pero se deja el del modelo: `low` bajaba la cita de la fuente al 83 % sin una mejora de latencia atribuible.
- Google retira y satura modelos: la lista con respaldo (`RESOLVO_GEMINI_MODELOS`) se sondea al arrancar y el log de Render dice cuál responde.
- Umbral del RAG `resolvo.rag.distancia-maxima` = 0,29. Desde el PR #55 se mide con 30 consultas con respuesta (4 con un ticket resuelto como fuente) y 10 sin ella. La recomendada es 0,310, pero la consulta sin respuesta más cercana está en 0,315: el margen es estrecho y r25 (0,305) se queda sin citas. El `application.yml` es la única fuente del valor.
- Citas de tickets resueltos: RS-4 y RS-5, los únicos de la semilla, repiten un artículo cada uno, así que desde el PR #63 los casos r33–r36 aceptan también ese artículo (`tambien` en `rag.jsonl`). Run 45833313132: cita 96,7 %, solo falla r25 por el umbral; p95 8,3 s. Para medir de verdad la cita de un ticket haría falta uno cuya respuesta no esté en ningún artículo.
- Búsqueda exacta por organización, sin HNSW (ADR-0007): vale para cientos o pocos miles de fragmentos por organización; si crece, particionar o `hnsw.iterative_scan` (sin comprobar en Neon).
- Tarifas: los alias `*-latest` y `gemini-3.5-flash` no tienen tarifa (coste `null`) y `gemini-2.5-flash` figura como obsoleto. Desde el PR #53 cada tarifa tiene fecha de inicio (`desde`) y cada llamada paga la vigente su día UTC; la subida de `gemini-3.8-flash` y `gemini-3.6-flash` del 2027-01-01 ya está en `application.yml`. Una subida nueva se añade como otra entrada con su `desde`.
- Limitaciones del ADR-0013 sin cubrir: el embedding de la consulta de «sugerir» corre dentro de la reserva, pero sus tokens no se cuentan, y la indexación por eventos no gasta presupuesto. Desde el PR #59 el cerrojo es por organización y el enmascarado tapa DNI y NIE con guion o espacio, teléfonos con puntos o prefijo 0034 e IBAN en minúsculas. `fragmento.texto` guarda los datos personales sin enmascarar (solo se enmascaran al enviar).

**Tests y valla**
- Desde el PR #61 los hooks resuelven la raíz por la ruta escrita (`postToolUse`) o por el `cwd` (`stop`), así que también corren en un worktree (`.cursor/hooks/raiz.ps1`, probado en `hooks.tests.ps1`). Ni esos tests ni `gates.tests.ps1` corren en CI.
- El gate rápido corre con `-Pfast` y no ejecuta los tests `full` (Spring, Docker): su RED no lo anota el gate. Desde el 2026-11-25 se anota en `.cursor/red.log` con el modo `manual`; desde el PR #61, `scripts/gates.ps1 red <ruta del test>` lo lanza y anota la línea solo si falla. Antes de esa fecha la mayoría de los RED de `full` no tienen línea o se reconstruyeron (detalle por fase en la bitácora).
- Los tests end to end (Playwright, `frontend/e2e/`, workflow [e2e.yml](.github/workflows/e2e.yml), PR #54) cubren cuatro flujos contra la pila real y **no forman parte** de `Gate obligatorio` (unos 2,5 min, casi todo preparación). En local: API en :8080 y `cd frontend && npm run e2e`. El resto de flujos se comprueba a mano.
- El gate `full` tarda unos 120-160 s en frío (tests y mutación), con techo de 300 s: vigilar si los tests `full` crecen. Los tests `full` limitan Hikari a 3 conexiones.

**Sin comprobar a mano** (solo tests): la demo en móvil. El resto de lo pendiente se comprobó el 2026-11-26 (detalle en la bitácora). La portada promete al Admin «las métricas de IA», pero la SPA no tiene esa página: solo existe `GET /api/metricas/ia`.
