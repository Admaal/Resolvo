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
