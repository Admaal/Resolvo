# Resolvo

Helpdesk B2B multi-tenant para equipos de soporte, en solitario y como portfolio. El producto está en [docs/producto.md](docs/producto.md) y las decisiones técnicas en [docs/adr/](docs/adr/).

Este archivo es la fuente de verdad de la fase y de la valla. El historial vive en [docs/fases.md](docs/fases.md).

## Fase actual

**Fase 2 — MVP desplegado con URL pública y login demo.** Hecha.

La demo corre en https://resolvo-6lc.pages.dev: SPA en Cloudflare Pages, proxy `/api` a la API en Render Free (Docker, Frankfurt) y Postgres en Neon Free. Semilla de demo idempotente (`V3`), aviso «despertando servidor…» y ping cada 5 minutos con GitHub Actions. Despliegue explicado en [README.md](README.md). Sin IA.

**La fase 3 no está empezada.** No hay RAG, ni pgvector, ni panel de IA.

## Stack

- Lenguaje y framework: Java 21 LTS con Spring Boot 3.5.16 (ADR-0001). Gradle wrapper 9.3.1 en `backend/`, un único módulo, paquete base `com.admaal.resolvo`.
- Tests: `backend\gradlew.bat -p backend test` (JUnit 5 + AssertJ + ArchUnit). El tag `full` marca lo que arranca Spring o Docker y queda fuera del bucle rápido.
- Dominio: `backend/src/main/java/com/admaal/resolvo/domain/` (sin Spring, sin JPA, sin Spring AI).
- Casos de uso: `backend/src/main/java/com/admaal/resolvo/application/` (solo depende de `domain` y de sus puertos).
- Adapters: `backend/src/main/java/com/admaal/resolvo/infrastructure/` (JPA, Flyway, JWT y HTTP).
- Lint de pureza: ArchUnit, en `backend/src/test/java/com/admaal/resolvo/architecture/`.
- Frontend: Angular 22.2.5 standalone, Angular Material y Vitest, en `frontend/`. El access token vive en un campo de `AuthService`.

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
6. **Rama `main`:** el repo es público (`github.com/Admaal/Resolvo`). Falta el ruleset que exige `Gate obligatorio` y prohíbe force push y borrado: se pone en Settings, Rules (`gh` no está instalado). Si el repo pasara a privado con cuenta gratuita, se usaría el fichero `.cursor/proteger-main`.

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
- `main` sigue sin ruleset: hasta que se ponga, un push directo lo salta todo.
- La contraseña de `neondb_owner` se compartió en un chat el 2026-11-14: conviene rotarla en Neon y actualizar `RESOLVO_DB_PASSWORD` y `RESOLVO_DB_MIGRATION_PASSWORD` en Render.
- Cuotas reales de Neon y Render sin anotar con fecha: la búsqueda web no estuvo disponible al cerrar la fase 2.
- En Render Free la API tarda unos 46 s en arrancar en frío (3 s en local); lo cubre el aviso de la SPA.
