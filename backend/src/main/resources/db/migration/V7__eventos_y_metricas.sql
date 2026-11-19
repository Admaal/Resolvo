-- Fase 5: bandeja de salida (outbox) de eventos, su clave de idempotencia y las llamadas a la IA.
-- Solo aditiva: no toca ninguna tabla anterior. Para deshacerla (Flyway Community no tiene undo):
--   drop table llamada_ia; drop table evento_procesado; drop table evento_outbox;

-- El evento se escribe en la misma transaccion que el cambio del ticket (ADR-0008). Sin claves
-- foraneas: la bandeja es un registro de lo ocurrido y no debe impedir borrar un ticket.
create table evento_outbox (
  id uuid primary key,
  tipo varchar(40) not null,
  organizacion_id uuid not null,
  ticket_id uuid not null,
  ocurrido_en timestamptz not null,
  estado varchar(20) not null default 'PENDIENTE',
  intentos int not null default 0,
  reclamado_hasta timestamptz,
  ultimo_error varchar(500)
);

create index evento_outbox_pendientes on evento_outbox (ocurrido_en, id) where estado = 'PENDIENTE';

-- Quien atendio que evento: la entrega repetida de un evento no repite el trabajo.
create table evento_procesado (
  consumidor varchar(80) not null,
  evento_id uuid not null,
  procesado_en timestamptz not null default now(),
  primary key (consumidor, evento_id)
);

-- Una fila por llamada al modelo de lenguaje, con exito o sin el. La organizacion es nula en las
-- llamadas que no son de nadie (por ejemplo el sondeo de arranque, que no se registra).
create table llamada_ia (
  id uuid primary key,
  organizacion_id uuid references organizacion (id),
  operacion varchar(40) not null,
  modelo varchar(80) not null,
  tokens_entrada int not null,
  tokens_salida int not null,
  milisegundos bigint not null,
  exito boolean not null,
  ocurrida_en timestamptz not null
);

create index llamada_ia_organizacion_fecha on llamada_ia (organizacion_id, ocurrida_en);
