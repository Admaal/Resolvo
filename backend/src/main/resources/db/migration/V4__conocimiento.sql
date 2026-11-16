-- Fase 3: base de conocimiento vectorial y sugerencias con citas (ADR-0006, ADR-0007).
-- Solo aditiva: no toca ninguna tabla anterior.
create extension if not exists vector;

create table articulo (
  id uuid primary key,
  organizacion_id uuid not null references organizacion (id),
  titulo varchar(200) not null,
  cuerpo varchar(8000) not null
);

create index articulo_organizacion_idx on articulo (organizacion_id);

-- Un fragmento es un trozo de una fuente (articulo o ticket) con su embedding.
-- fuente_id no lleva clave foranea: apunta a articulo o a ticket segun fuente.
-- Reindexar una fuente reemplaza sus fragmentos; la unicidad evita duplicados por reintento.
create table fragmento (
  id uuid primary key,
  organizacion_id uuid not null references organizacion (id),
  fuente varchar(20) not null,
  fuente_id uuid not null,
  hash_fuente varchar(64) not null,
  modelo varchar(80) not null,
  orden integer not null,
  titulo varchar(250) not null,
  texto varchar(8000) not null,
  embedding vector(768) not null,
  unique (fuente, fuente_id, modelo, orden)
);

create index fragmento_organizacion_idx on fragmento (organizacion_id);
create index fragmento_embedding_idx on fragmento using hnsw (embedding vector_cosine_ops);

create table sugerencia (
  id uuid primary key,
  organizacion_id uuid not null references organizacion (id),
  ticket_id uuid not null references ticket (id),
  borrador varchar(8000) not null,
  estado varchar(20) not null,
  modelo varchar(80) not null,
  creada_en timestamptz not null,
  resuelta_por uuid references usuario (id),
  resuelta_en timestamptz
);

-- Una sola sugerencia pendiente por ticket: el doble clic no duplica ni gasta cuota.
create unique index sugerencia_una_pendiente_por_ticket on sugerencia (ticket_id) where estado = 'PENDIENTE';

-- La cita guarda una copia de la fuente (tipo, id y titulo) porque reindexar reemplaza los fragmentos.
create table cita (
  sugerencia_id uuid not null references sugerencia (id),
  orden integer not null,
  fragmento_id uuid not null,
  fuente varchar(20) not null,
  fuente_id uuid not null,
  titulo varchar(250) not null,
  primary key (sugerencia_id, orden)
);
