-- Reservas abiertas del presupuesto diario de IA, en la base en vez de en memoria: una llamada al
-- modelo en curso ocupa su hueco aunque la atienda otra instancia. Cada reserva caduca sola por si
-- la instancia que la abrio muere sin cerrarla. Sin clave ajena: es estado de paso, no historia.
create table presupuesto_reserva (
  id uuid primary key,
  organizacion_id uuid not null,
  caduca_en timestamptz not null
);

create index presupuesto_reserva_organizacion on presupuesto_reserva (organizacion_id, caduca_en);
