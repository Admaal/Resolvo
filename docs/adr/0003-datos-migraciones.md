# ADR-0003: Postgres, pgvector y Flyway

- Fecha: 2026-10-31
- Estado: aceptada

## Contexto

El sistema necesita datos relacionales con aislamiento por organización y, en la fase 3, búsqueda vectorial sobre documentación y tickets resueltos. El hosting es Neon Free (ADR-0009), donde el proyecto se crea y se destruye sin coste. En Solvo, el esquema se mantenía con `ddl-auto=update` más un SQL manual pegado en el panel del proveedor: el esquema real dejó de estar en el repositorio.

## Opciones

1. **Postgres + pgvector con Flyway** — a favor: un solo motor para datos y vectores; el esquema versionado vive en el repo y se aplica igual en local, CI y demo; índices HNSW disponibles. En contra: la dimensión del vector se fija en la migración y cambiarla exige reindexar.
2. **Postgres + base vectorial aparte (Qdrant, Pinecone)** — a favor: búsqueda vectorial más especializada. En contra: un servicio y una cuota más, dos fuentes de verdad que sincronizar, y un fallo de red nuevo en el camino crítico.
3. **Liquibase en lugar de Flyway** — a favor: changelogs en XML/YAML y rollbacks declarativos. En contra: más indirección para migraciones SQL sencillas; Flyway es lo que ya conoce el mercado objetivo.

## Decisión

Postgres con la extensión `vector`, migraciones Flyway desde `V1` en `backend/src/main/resources/db/migration/`, y `ddl-auto=validate` siempre. El esquema nunca se genera desde las entidades.

## Consecuencias

- Toda tabla de negocio lleva `organizacion_id` y toda consulta filtra por él; las pruebas de aislamiento usan dos identidades.
- La dimensión del embedding se fija en una migración concreta; el modelo queda abierto en ADR-0007, pero cambiarlo implica migración y reindexado.
- Ninguna migración aplicada se edita: los cambios van en una versión nueva.
