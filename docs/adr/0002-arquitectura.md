# ADR-0002: Monolito modular hexagonal

- Fecha: 2026-10-31
- Estado: aceptada

## Contexto

El dominio tiene reglas que no deben depender de Spring, JPA ni del proveedor de LLM: transiciones de estado, cálculo de SLA, aislamiento por organización y el flujo de sugerencia con aprobación humana. A la vez, el proyecto lo mantiene una sola persona en horas sueltas, y la fase 1 debe caber en cuatro semanas.

## Opciones

1. **Monolito modular hexagonal** — a favor: un solo despliegue, dominio puro testeable sin contexto de Spring, puertos que aíslan base de datos, LLM y mensajería; las reglas sobreviven a un cambio de proveedor. En contra: más carpetas y mappers que un proyecto en capas.
2. **Microservicios** — a favor: despliegue y escalado independientes. En contra: multiplica infraestructura, contratos y fallos de red para un producto que aún no tiene usuarios; imposible de sostener a tiempo parcial.
3. **Capas clásicas (controller → service → repository)** — a favor: menos ficheros y arranque más rápido. En contra: el dominio acaba importando JPA y Spring, y con ello llega el `ddl-auto` y los tests que necesitan contexto.

## Decisión

Monolito modular con arquitectura hexagonal, verificado con ArchUnit: `domain` no importa Spring, JPA ni Spring AI; `application` solo depende de `domain`; `infrastructure` implementa los puertos de salida. Spring AI queda confinado a `infrastructure/ai`.

## Consecuencias

- Cada regla de `AGENTS.md` enlaza con la regla de ArchUnit que la comprueba.
- Cambiar de proveedor de LLM es añadir un adapter, no tocar casos de uso.
- Hay mappers explícitos entre entidades JPA y modelo de dominio; es código de más que se acepta a cambio de tests de dominio sin arrancar Spring.
