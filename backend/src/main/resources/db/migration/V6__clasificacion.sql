-- Fase 4: propuestas de clasificacion (categoria y prioridad) con aprobacion humana (ADR-0006).
-- Solo aditiva: no toca ninguna tabla anterior.
create table clasificacion (
  id uuid primary key,
  organizacion_id uuid not null references organizacion (id),
  ticket_id uuid not null references ticket (id),
  categoria_id uuid not null references categoria (id),
  prioridad varchar(20) not null,
  motivo varchar(500) not null,
  estado varchar(20) not null,
  modelo varchar(80) not null,
  creada_en timestamptz not null,
  resuelta_por uuid references usuario (id),
  resuelta_en timestamptz
);

-- Una sola clasificacion pendiente por ticket: el indice impide duplicarla aunque dos peticiones lleguen
-- a la vez. La aplicacion evita ademas la segunda llamada al modelo, pero solo dentro de un proceso.
create unique index clasificacion_una_pendiente_por_ticket on clasificacion (ticket_id) where estado = 'PENDIENTE';
