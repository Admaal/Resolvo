# ADR-0010: Tests y valla

- Fecha: 2026-10-31
- Estado: aceptada

## Contexto

El proyecto debe demostrar entrega disciplinada: dominio testeado sin Spring, aislamiento entre organizaciones probado con dos identidades y un gate que impida fusionar código sin tests. En Solvo no había hooks ni gates y las cifras de cobertura del `AGENTS.md` no salían de ningún sitio.

## Opciones

1. **JUnit 5 + Testcontainers + ArchUnit + PIT en dominio + Playwright** — a favor: cubre los cuatro niveles que interesan (unitario rápido, arquitectura, integración real con Postgres y flujo de navegador); PIT limitado al dominio mantiene el coste asumible. En contra: PIT es lento y Testcontainers exige Docker en cada máquina y en CI.
2. **Solo tests unitarios con mocks** — a favor: rápido. En contra: los mocks no demuestran aislamiento por organización ni que Flyway y Postgres acepten el esquema.
3. **Cobertura como único umbral** — a favor: simple de configurar. En contra: mide líneas ejecutadas, no comportamiento; se puede subir cobertura sin comprobar una sola regla.

## Decisión

- Dominio y casos de uso: JUnit 5 sin contexto de Spring.
- Arquitectura: ArchUnit en backend y regla de imports en ESLint en el frontend.
- Integración: Testcontainers con Postgres + pgvector y, en el perfil de eventos, Redpanda.
- Mutación: PIT solo sobre `domain` y `application`, umbral 80 %.
- End to end: Playwright sobre el flujo crear ticket → sugerencia → aprobar.
- Todo eso se invoca desde `scripts/gates.ps1`, la misma fuente para los hooks y para CI.

## Consecuencias

- El umbral de mutación y el presupuesto del bucle rápido se miden y se anotan en `AGENTS.md` con fecha.
- Los tests de IA usan stub u Ollama; nunca un proveedor de pago.
- El runner unitario de Angular (Karma o Jest) queda abierto hasta crear el esqueleto.
- Montar esta valla es el siguiente paso del proyecto, no parte de este arranque.

## Enmienda (2026-11-25)

El end to end con Playwright no se llegó a montar. La SPA se cubre con tests de componente en Vitest (más de 200) y los flujos de punta a punta se comprobaron a mano en la demo desplegada y en local contra la API real, anotado con fecha en `AGENTS.md`. Queda como hueco conocido: ningún test automático recorre el navegador contra la API. El runner de Angular es Vitest, el de serie del CLI desde la versión 21.

## Enmienda (2026-11-26)

Se monta el end to end que la enmienda anterior dejó como hueco. Playwright con cuatro flujos en `frontend/e2e/` (entrar con la demo y cambiar de vista, abrir un ticket y comentar, lo que ve y no ve un CLIENTE, y la barra de vistas abajo en móvil), en escritorio y en un viewport de 390 px. Corre contra la pila real: Postgres con pgvector, la API (el jar, sin perfil `gemini`) y la SPA con `ng serve`. Va en su propio workflow, `e2e.yml`, y **no** forma parte de `Gate obligatorio`: tarda minutos (dependencias, Chromium y arranque de la API) y un fallo del navegador no debe bloquear cada push. Si se demuestra estable, puede pasar al gate. En local: API en `:8080` y `npm run e2e` desde `frontend/` (con `PW_CHANNEL=msedge` usa el Edge instalado).

