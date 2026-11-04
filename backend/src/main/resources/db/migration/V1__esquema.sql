create table organizacion (
  id uuid primary key,
  nombre varchar(120) not null
);

create table usuario (
  id uuid primary key,
  organizacion_id uuid not null references organizacion (id),
  email varchar(320) not null unique,
  rol varchar(20) not null,
  hash_contrasena varchar(100) not null,
  fallos_seguidos integer not null,
  bloqueado_hasta timestamptz
);

create table categoria (
  id uuid primary key,
  organizacion_id uuid not null references organizacion (id),
  nombre varchar(80) not null
);

create table secuencia_ticket (
  organizacion_id uuid primary key references organizacion (id),
  ultimo integer not null
);

create table ticket (
  id uuid primary key,
  organizacion_id uuid not null references organizacion (id),
  autor_id uuid not null references usuario (id),
  categoria_id uuid not null references categoria (id),
  prioridad varchar(20) not null,
  codigo varchar(40) not null,
  asunto varchar(200) not null,
  cuerpo varchar(8000) not null,
  abierto_en timestamptz not null,
  vence_el timestamptz not null,
  estado varchar(20) not null,
  asignado_id uuid references usuario (id),
  unique (organizacion_id, codigo)
);

create table comentario (
  id uuid primary key,
  ticket_id uuid not null references ticket (id),
  autor_id uuid not null,
  cuerpo varchar(8000) not null,
  interno boolean not null,
  creado_en timestamptz not null
);

create table bono_refresh (
  token_hash varchar(64) primary key,
  usuario_id uuid not null references usuario (id),
  familia uuid not null,
  consumido boolean not null,
  revocado boolean not null
);
