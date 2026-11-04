# ADR-0008: Outbox transaccional y cola en Postgres

- Fecha: 2026-10-31
- Estado: aceptada

## Contexto

La creación de un ticket debe disparar trabajo posterior (clasificación, sugerencia) sin perder eventos si el proceso cae, y sin obligar al usuario a esperar a la IA. El plan original quería enseñar mensajería tipo Kafka sin pagar un servicio gestionado.

## Opciones

1. **Outbox transaccional + cola en Postgres online, Redpanda en local** — a favor: el evento se escribe en la misma transacción que el ticket, así que no hay eventos fantasma ni pérdidas; en la demo no hay servicio extra que pagar ni que se duerma; Redpanda en compose permite enseñar la API de Kafka en local con Testcontainers. En contra: dos implementaciones del mismo puerto (`PublicadorEventos`) que hay que mantener.
2. **Kafka gestionado (Confluent, Redpanda Cloud)** — a favor: la misma tecnología en local y en producción. En contra: free tier limitado, otra cuenta y otra tarjeta, y un servicio más que puede dormirse o caducar.
3. **Sin eventos: llamada síncrona al clasificador** — a favor: el código más corto. En contra: el usuario espera al LLM, un fallo de IA rompe la creación del ticket y no queda rastro para reintentar.

## Decisión

Patrón outbox: la misma transacción inserta el ticket y su evento; un poller publica y marca. En local, adapter Kafka contra Redpanda; online, adapter Postgres con `FOR UPDATE SKIP LOCKED`. Los consumidores son idempotentes con clave de idempotencia en `eventos_procesados`.

## Consecuencias

- Los tests de eventos cubren que el mismo evento procesado dos veces no genera dos sugerencias.
- La fase 1 no usa eventos: la columna y la tabla llegan con la migración de la fase 5, cuando hay algo que consumir.
- El puerto `PublicadorEventos` no depende de `spring-kafka`, que solo se activa con el perfil `kafka`.
