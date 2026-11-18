# Resolvo

Helpdesk B2B multi-tenant para equipos de soporte, en solitario y como portfolio. El producto está en [docs/producto.md](docs/producto.md) y las decisiones técnicas en [docs/adr/](docs/adr/).

Este archivo es la fuente de verdad de la fase y de la valla. El historial vive en [docs/fases.md](docs/fases.md).

## Fase actual

**Fase 2 — MVP desplegado con URL pública y login demo.** Hecha.

La demo corre en https://resolvo-6lc.pages.dev: SPA en Cloudflare Pages, proxy `/api` a la API en Render Free (Docker, Frankfurt) y Postgres en Neon Free. Semilla de demo idempotente (`V3`), aviso «despertando servidor…» y ping cada 5 minutos con GitHub Actions. Despliegue explicado en [README.md](README.md). Sin IA.

**Fase 3 — RAG con pgvector, sugerencias con citas y aprobación humana.** Hecha el 2026-11-16, con la IA activa en la demo: Render corre con `SPRING_PROFILES_ACTIVE=gemini` y `RESOLVO_GEMINI_API_KEY`, Neon tiene V4 y V5 (`create extension vector` funciona) con los 8 artículos y los tickets resueltos indexados con `gemini-embedding-001`, y la SPA con el panel está desplegada en Pages. Comprobado de extremo a extremo en producción: un agente pidió una sugerencia para un ticket y la aprobó; quedó una sugerencia `APROBADA` con `gemini-flash-lite-latest`, 1 cita y 1 comentario público. Historial en [docs/fases.md](docs/fases.md).

La IA es un perfil: sin `gemini` no se carga ninguna autoconfiguración de Spring AI y los endpoints de IA responden 503 (ADR-0006). Embeddings `gemini-embedding-001` a 768 (ADR-0007).

**Fase 4 — clasificación sugerida y evals en CI.** Código hecho en la rama `claude/planificacion-fase-4-6a6311`, **sin fusionar ni desplegar** (2026-11-16). Un AGENTE o ADMIN pide una clasificación del ticket (`POST /api/tickets/{id}/clasificaciones`), la corrige y la aplica o la descarta; aplicar recalcula el SLA. Una pendiente por ticket (V6, índice único parcial). Los evals (`backend\gradlew.bat -p backend evals`, workflow [evals.yml](.github/workflows/evals.yml)) llaman a Gemini real y **no forman parte** de `Gate obligatorio` (ADR-0011). Falta, y es de quien tenga las claves: crear el secreto `RESOLVO_GEMINI_API_KEY` en GitHub (la IA de la fase 3 ya está activa en Render), correr los evals una vez y fijar con esa corrida los umbrales de `evals/umbrales.json` y `RESOLVO_RAG_DISTANCIA_MAXIMA`; desplegar la SPA a mano. Cuando eso esté comprobado se añade la fila a [docs/fases.md](docs/fases.md).

## Stack

- Lenguaje y framework: Java 21 LTS con Spring Boot 3.5.16 (ADR-0001). Gradle wrapper 9.3.1 en `backend/`, un único módulo, paquete base `com.admaal.resolvo`.
- Tests: `backend\gradlew.bat -p backend test` (JUnit 5 + AssertJ + ArchUnit). El tag `full` marca lo que arranca Spring o Docker y queda fuera del bucle rápido.
- Dominio: `backend/src/main/java/com/admaal/resolvo/domain/` (sin Spring, sin JPA, sin Spring AI).
- Casos de uso: `backend/src/main/java/com/admaal/resolvo/application/` (solo depende de `domain` y de sus puertos).
- Adapters: `backend/src/main/java/com/admaal/resolvo/infrastructure/` (JPA, JDBC y pgvector, Flyway, JWT, HTTP y `ai/` con Spring AI).
- Lint de pureza: ArchUnit, en `backend/src/test/java/com/admaal/resolvo/architecture/`.
- Frontend: Angular 22.2.5 standalone, Angular Material y Vitest, en `frontend/`. El access token vive en un campo de `AuthService`. El panel de sugerencias (`features/tickets/sugerencia-panel.ts`) solo se pinta a AGENTE y ADMIN.

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

## Riesgos anotados

- La mutación sobre dominio real quedó en 98 % (121/124) el 2026-11-04.
- `gh` 2.104 está instalado en `C:\Program Files\GitHub CLI` y con sesión como `Admaal`: los PRs, su CI y el merge se hacen con él (`gh pr create`, `gh pr merge --merge`).
- La contraseña de `neondb_owner` se compartió en un chat el 2026-11-14: conviene rotarla en Neon y actualizar `RESOLVO_DB_PASSWORD` y `RESOLVO_DB_MIGRATION_PASSWORD` en Render.
- Cuotas reales de Neon y Render sin anotar con fecha: la búsqueda web no estuvo disponible al cerrar la fase 2.
- En Render Free la API tarda unos 47 s en arrancar en frío con Spring AI (medido el 2026-11-16 en el despliegue de la fase 3; antes unos 46 s) frente a 3 s en local; lo cubre el aviso de la SPA. En local la imagen con 512 MB arranca en 3,2 s (203 MiB sin IA, 213 MiB con el perfil `gemini`).
- **Cloudflare Pages no está conectado a Git** (`wrangler pages project list` dice `Git Provider: No`): un merge a `main` despliega la API en Render pero no la SPA. Cada cambio de `frontend/` se despliega a mano: `cd frontend && npm run build && npx wrangler pages deploy --project-name resolvo --branch main`. Sin `--branch main` el despliegue sale como preview y no toca `resolvo-6lc.pages.dev`. Necesita `wrangler login` con la cuenta de Cloudflare.
- En el log de Render aparece `Required classpath entry does not exist: extraido/app.jar` y `Unable to use shared archive`: el archivo CDS no se usa en producción y el arranque en frío de 47 s es sin CDS. Causa sin investigar (ADR-0009 daba por buena la imagen con CDS).
- Los gates crecieron con la fase 3 (2026-11-16, Windows, Docker en marcha): 173 tests de backend, 0 fallos y 0 saltados; mutación sobre 217 mutantes con 202 muertos más 6 expirados o sin memoria que PIT cuenta como detectados (96 % según PIT, 93,1 % contando solo los muertos). Muy por encima del 80 %. El gate `full` tardó 28,6 s y la mutación 35,9 s antes de las correcciones de la verificación, frente a los 13 s de la mutación del 2026-11-04; techo de 300 s. Los tests `full` limitan Hikari a 3 conexiones porque cada contexto distinto de Spring guarda su pool.
- Umbral de similitud: ya existe (`resolvo.rag.distancia-maxima`, `RESOLVO_RAG_DISTANCIA_MAXIMA`, distancia coseno 0 a 2) pero su valor por defecto es 2.0, que no descarta nada, porque no se puede calibrar sin una corrida real de los evals. Hasta entonces siempre se citan los fragmentos más cercanos aunque no sean relevantes. El eval imprime el umbral que separaría las consultas con y sin respuesta.
- El modelo de redacción `gemini-2.5-flash`, fijo en el yaml, dio 404 en producción el 2026-11-16 («no longer available to new users»): ningún test lo podía detectar sin una clave real. Ahora hay una lista de modelos con respaldo (`RESOLVO_GEMINI_MODELOS`), sondeada en paralelo al arrancar y ordenada por latencia, 15 s por intento, máximo 3 modelos por petición y reintentos de Spring AI acotados a 2 (el valor por defecto dejaba la petición colgada minutos). El primer despliegue mostró que `gemini-flash-latest`, `gemini-3.5-flash` y `gemini-3.8-flash` no contestaban en 20 s desde Render y que `gemini-2.5-flash-lite` también estaba retirado: el log de arranque de Render dice qué responde.
- La búsqueda es exacta dentro de la organización y no usa el índice HNSW (ADR-0007): el HNSW filtra por organización después de elegir vecinos y con otra organización mucho mayor devolvía 0 filas. Vale para cientos o pocos miles de fragmentos por organización; si crece, hay que particionar o usar `hnsw.iterative_scan` (pgvector 0.8, sin comprobar en Neon).
- Los hooks de Claude Code no ven los worktrees: ejecutan el script del checkout principal y la ruta del fichero no coincide con ningún prefijo, así que `postToolUse` sale con exit 0 sin lanzar tests y no anota RED. En un worktree hay que ejecutar `scripts/gates.ps1 fast <ruta>` a mano. Sin arreglar para no tocar la valla dentro de una feature.
- RED de la fase 3: se observaron fallar en la sesión (casi todos por compilación, al no existir aún la clase), pero el hook no los anotó en el worktree. El 2026-11-16 se **reconstruyeron** apartando la producción de cada test y lanzando `scripts/gates.ps1 fast`: son las líneas de `.cursor/red.log` con sha `90f30e7` fechadas 12:34 a 12:36 (más dos de la corrección del rol y dos del delimitador del prompt, estas sí reales). Prueban que el test falla sin su producción, no el orden en que se escribieron. Los tests `full` (pgvector, sugerencias JDBC, HTTP, concurrencia y perfiles de Gemini) no pasan por el gate rápido y no tienen línea.
- Una línea RED de `RedactorSpringAiTest` de las 12:23 es un error de compilación de un test mal escrito, no un fallo del comportamiento; la válida es la de las 12:24.
- Reabrir un ticket resuelto no borra sus fragmentos del índice.
- Fase 4 (2026-11-16): el runner `EvalIaTest` solo se comprobó sin clave (falla con un mensaje claro y sin tocar la red). Su lógica contra Gemini real, el SQL de distancias y los umbrales (`evals/umbrales.json`, cifras iniciales del plan) **no están verificados**: la primera corrida real puede pedir ajustes, y bajar un umbral después cuenta como debilitar un test (`UmbralesTest` fija los suelos).
- Los evals no calculan coste en euros: no hay tabla de precios de lista versionada y no se inventan. Miden tokens por llamada (límite 3000) y p95 de latencia (8 s); el free tier cuesta 0 €.
- RED de la fase 4: los tests unitarios de dominio, aplicación y `infrastructure/ai` y las specs de componente de la SPA se escribieron y se observaron fallar con `scripts/gates.ps1 fast` a mano antes de la producción (líneas de `.cursor/red.log` con sha `c24c98f` y `8408440`, anotadas por el gate al lanzarlo yo a mano porque el hook no ve el worktree). Tras el verificador: el suelo de citas de `UmbralesTest`, el cuerpo incompleto de `AplicarClasificacionTest` y `dobleClicNoDuplica` (una sola llamada al modelo) tienen RED real; el de `InformeEvalTest.elInformeNoIncluyeLaClave` se **reconstruyó** anulando `InformeEval.ocultar` y se restauró. **Sin línea de RED**: `ConcurrenciaClasificacionTest.dosAplicacionesSimultaneasAplicanUnaSolaVez`, `ClasificacionSinIaHttpTest`, `ClasificacionesJdbcTest`, `ClasificacionHttpTest` (salvo su RED de compilación), el umbral de `BaseConocimientoPgvectorTest`, `CasosDeEvalTest` y `clasificaciones.service.spec.ts` (el fast no corre los `full` y el RED de la SPA se anotó solo para el spec del panel). El primer RED del frontend (19:01) fue falso: faltaba `node_modules` y `ng` no existía; el real es el de 19:01:42.
- Gates con la fase 4 (2026-11-16, Windows, Docker en marcha, sin caché, tras fusionar `main` y corregir al verificador): 305 tests de backend y 51 de frontend, 0 fallos y 0 saltados; `gates.ps1 full` 113 s de tests y 41 s de mutación (267 mutantes, 251 muertos más 6 expirados o sin memoria; 96 % según PIT, 94,0 % contando solo los muertos). Techo 300 s: quedan unos 145 s de margen en frío; vigilar si los tests `full` crecen.
- Cuota de Gemini, nivel gratuito, proyecto `Proptech` de AI Studio, mirada el 2026-11-16: `gemini-3.1-flash-lite` 15 solicitudes/min, 250 K tokens/min y 500 solicitudes/día; `gemini-2.5-flash-lite` 10/min y 20/día; `gemini-2.5-flash` y `gemini-3-flash` 5/min y 20/día. Sobra para la demo. No se vieron los límites de `gemini-flash-lite-latest`, `gemini-3.5-flash-lite` ni de embeddings. La cuota es por proyecto: si `Proptech` se usa para otra cosa, se comparte. En el sondeo de arranque de ese día respondieron los modelos lite (5-6 s por llamada mínima) y los demás no contestaron en 15 s desde Render.
- La clave de Gemini que se usó para diagnosticar se pegó en un chat el 2026-11-16: conviene revocarla en AI Studio si no se ha hecho y confirmar que la de Render es la nueva.
