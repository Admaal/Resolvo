# ADR-0004: Angular standalone + Material

- Fecha: 2026-10-31
- Estado: aceptada

## Contexto

La interfaz es una SPA para tres roles (CLIENTE, AGENTE, ADMIN) sobre una API REST. El mercado al que apunta el portfolio concentra el combo Java + Angular, y el hosting elegido es Cloudflare Pages, que sirve estáticos sin ejecutar servidor (ADR-0009).

## Opciones

1. **Angular standalone + Material** — a favor: combo típico en banca y consultoría española; standalone reduce NgModules; Material resuelve tablas, formularios y diálogos, y la apariencia visual vive en [docs/design.md](../design.md); build estático compatible con Pages. En contra: más peso de framework y curva inicial mayor que una librería ligera.
2. **React** — a favor: ecosistema mayor y más ofertas internacionales. En contra: rompe el relato Java + Angular del portfolio y obliga a elegir y mantener un stack de UI propio.
3. **Angular con NgModules** — a favor: patrón clásico conocido en empresas. En contra: es el modelo que Angular está dejando atrás; standalone es el camino actual.

## Decisión

Angular standalone con Angular Material, `core/` para auth e interceptores y `features/` para tickets, sugerencias y métricas. La regla de imports del frontend vive en ESLint. El diseño visual de la web se hace a partir de [docs/design.md](../design.md): tokens, layout, componentes, motion y accesibilidad salen de ese documento. Material aporta los controles; la apariencia la marca el diseño.

## Consecuencias

- El build se publica como estáticos en Cloudflare Pages; el `API_URL` se inyecta por configuración de build.
- La versión de Angular se fija al crear el esqueleto y no se sube de mayor a mitad del plan.
- El runner de tests unitarios (Karma o Jest) queda abierto en `docs/producto.md`.
- Cualquier pantalla nueva sigue [docs/design.md](../design.md). Si el diseño y este ADR se contradicen, manda el diseño en lo visual y este ADR en el stack.
- Cerrado el 2026-11-04: Angular 22.2.5 y Vitest. No se sube de mayor a mitad del plan.
- Cerrado el 2026-11-14: no hay `API_URL` en build. La SPA sigue llamando a `/api/...` y una Pages Function (`frontend/functions/api/[[path]].ts`) reenvía a la API con la variable `API_ORIGIN`. Mismo origen, sin CORS.
