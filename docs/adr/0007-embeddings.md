# ADR-0007: Un modelo de embeddings y una dimensión fijos

- Fecha: 2026-10-31
- Estado: aceptada

## Contexto

La búsqueda vectorial vive en la misma base de datos (ADR-0003), así que la dimensión del vector es una decisión de esquema: una columna `vector(N)` y un índice HNSW. El proyecto debe poder migrar de proveedor de chat (ADR-0006) sin reindexar nada; la dimensión atada al modelo de embeddings rompe esa libertad, y conviene asumirlo de forma explícita.

## Opciones

1. **Un modelo y una dimensión fijos** — a favor: una sola columna, un solo índice, comparaciones directas; el código de ingesta y búsqueda es simple. En contra: cambiar de modelo de embeddings exige migración y reindexado completo.
2. **Varias columnas, una por modelo** — a favor: convivencia y comparación sin migrar. En contra: multiplica índices y almacenamiento, y obliga a decidir en cada consulta qué columna usar sin que nadie lo pida.
3. **Dimensión dinámica calculada al indexar** — a favor: máxima libertad. En contra: Postgres no admite columnas `vector` de dimensión variable en un índice HNSW; llevaría a guardar todo como texto y perder la búsqueda indexada.

## Decisión

Un único modelo de embeddings con una dimensión `N` fija, declarada en la migración vectorial y en el modelo de dominio de `chunks`. El modelo concreto y `N` se cierran antes de esa migración y se anotan en este ADR.

## Consecuencias

- Cambiar de modelo de embeddings es una decisión con migración y reindexado, no un cambio de perfil.
- Los documentos guardan qué modelo los indexó para detectar contenido desfasado tras un cambio.
- `docs/producto.md` mantiene el modelo y `N` como pregunta abierta hasta la fase 3.
