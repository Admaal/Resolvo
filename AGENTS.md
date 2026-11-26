# Resolvo

Helpdesk B2B multi-tenant para equipos de soporte, en solitario y como portfolio. El producto está en [docs/producto.md](docs/producto.md) y las decisiones técnicas en [docs/adr/](docs/adr/).

Este archivo es la fuente de verdad de la fase, de la valla y de los riesgos abiertos. El historial resumido vive en [docs/fases.md](docs/fases.md) y el detalle en [docs/bitacora.md](docs/bitacora.md).

## Fase actual

**Fases 0 a 6 hechas y desplegadas** (la 6 el 2026-11-19), con pulido posterior hasta el 2026-11-25: rediseño de la SPA, moderación de comentarios, código del ticket en la URL, sesión renovada al caducar el token, visibilidad por autor para el CLIENTE y la auditoría de portfolio. **No hay fase en curso.** Resumen por fase en [docs/fases.md](docs/fases.md); el detalle (qué se comprobó en la demo, RED, verificadores, cifras de cada gate) en [docs/bitacora.md](docs/bitacora.md).

La demo corre en https://resolvo-6lc.pages.dev: SPA en Cloudflare Pages con una Pages Function que hace de proxy `/api` a la API en Render Free (Docker, Frankfurt) y Postgres con pgvector en Neon Free. Migraciones V1 a V8 aplicadas en Neon; V9 (artículo semilla del login) está en la rama de la auditoría, sin desplegar. Ping cada 5 minutos con GitHub Actions.

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
- Frontend: Angular 22.2.5 standalone, Angular Material y Vitest, en `frontend/`. El access token vive en un campo de `AuthService`. El panel de sugerencias (`features/tickets/sugerencia-panel.ts`) solo se pinta a AGENTE y ADMIN.
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
- Arranque en frío en Render de unos 47 s con Spring AI (3 s en local). El archivo CDS no se usa en producción (`Required classpath entry does not exist: extraido/app.jar`); causa sin investigar. Lo cubre el aviso de la SPA.
- Los límites de uso (IA, login, comentarios) y el presupuesto diario viven en memoria: un reinicio de Render los pone a cero y dos instancias sumarían el cupo. El 429 del 11.º comentario no saltó una vez en la demo sin causa explicada (hipótesis: dos instancias durante un relevo); `RESOLVO_LIMITE_COMENTARIOS_POR_MINUTO=10` está puesta explícitamente en Render.
- Tras despliegues seguidos se han visto 502 de `/api/salud` y 500 de un chunk de JS en Pages, pasajeros; sin investigar.
- El procesador de eventos solo corre dentro de la API: si Render se duerme con eventos pendientes, esperan al siguiente arranque. Una fila `FALLIDO` de `evento_outbox` no se reintenta sola. Revertir el código con eventos `COMENTARIO_MODERADO` pendientes rompería la lectura por tipo desconocido.
- Cuotas reales de Neon y Render sin anotar con fecha. La cuota de Gemini es la del nivel gratuito del proyecto `Proptech` de AI Studio y se comparte si ese proyecto se usa para otra cosa.

**IA**
- Latencia: p95 de los evals entre 1,5 y 12,7 s según la corrida; el umbral está en 15 s (ADR-0011), una decisión sobre el listón, no una mejora. Bajarla (por ejemplo limitando el razonamiento) sigue sin hacerse.
- Google retira y satura modelos: la lista con respaldo (`RESOLVO_GEMINI_MODELOS`) se sondea al arrancar y el log de Render dice cuál responde.
- Umbral del RAG `resolvo.rag.distancia-maxima` = 0,29, calibrado con 16 consultas con respuesta y 6 sin ella, solo sobre artículos: margen estrecho y sin medir con tickets resueltos como fuente. El `application.yml` es la única fuente del valor.
- Búsqueda exacta por organización, sin HNSW (ADR-0007): vale para cientos o pocos miles de fragmentos por organización; si crece, particionar o `hnsw.iterative_scan` (sin comprobar en Neon).
- Tarifas: los alias `*-latest` y `gemini-3.5-flash` no tienen tarifa (coste `null`); `gemini-3.8-flash` y `gemini-3.6-flash` duplican precio el 2027-01-01 y `gemini-2.5-flash` figura como obsoleto. Revisar `application.yml` antes de esa fecha.
- Limitaciones del ADR-0013 sin cubrir: embeddings fuera del presupuesto, cerrojo global con consulta a la base, enmascarado sin DNI con guion ni teléfonos con puntos. `fragmento.texto` guarda los datos personales sin enmascarar (solo se enmascaran al enviar).
- Las sugerencias ya generadas no cambian si cambia su fuente: el borrador pendiente de RS-1 en la demo repite el texto viejo del artículo de login.

**Tests y valla**
- Los hooks de Claude Code no ven los worktrees: `postToolUse` sale con exit 0 sin lanzar tests y no anota RED. En un worktree hay que ejecutar `scripts/gates.ps1 fast <ruta>` a mano.
- El gate rápido corre con `-Pfast` y no ejecuta los tests `full` (Spring, Docker): su RED no lo anota el gate. Desde el 2026-11-25 se anota a mano en `.cursor/red.log` con el modo `manual` en lugar de `fast`, tras verlo fallar con `gradlew --tests`. Antes de esa fecha la mayoría de los RED de `full` no tienen línea o se reconstruyeron (detalle por fase en la bitácora).
- Sin tests end to end de navegador (enmienda de ADR-0010): los flujos completos se comprueban a mano.
- El gate `full` tarda unos 120-160 s en frío (tests y mutación), con techo de 300 s: vigilar si los tests `full` crecen. Los tests `full` limitan Hikari a 3 conexiones.
- La SPA solo carga la primera página (20) del listado de tickets.
- Las columnas `fallos_seguidos` y `bloqueado_hasta` de `usuario` siguen sin uso desde que se retiró el bloqueo de cuenta.

**Sin comprobar a mano** (solo tests): la renovación de sesión fallida y dos 401 simultáneos; que ocultar o restaurar un comentario de un ticket resuelto reindexe de verdad; el límite de login con dos clientes de IPs distintas; la demo en móvil y como Admin más allá de la bandeja; el alta de un ticket y `J`/`K` en la demo.
