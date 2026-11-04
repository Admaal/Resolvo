-- Datos ficticios de demostración. La contraseña de los tres usuarios es demo-resolvo.
insert into organizacion (id, nombre)
values ('11111111-1111-1111-1111-111111111111', 'Acme')
on conflict (id) do nothing;

insert into usuario (id, organizacion_id, email, rol, hash_contrasena, fallos_seguidos)
values
  ('22222222-2222-2222-2222-222222222221', '11111111-1111-1111-1111-111111111111', 'cliente@acme.test', 'CLIENTE', '$2a$10$IDJv9Q.zcK00ENEtRJFwVeWZNTJPUfs25uMQkslSvLJ.rYOMisPem', 0),
  ('22222222-2222-2222-2222-222222222222', '11111111-1111-1111-1111-111111111111', 'agente@acme.test', 'AGENTE', '$2a$10$IDJv9Q.zcK00ENEtRJFwVeWZNTJPUfs25uMQkslSvLJ.rYOMisPem', 0),
  ('22222222-2222-2222-2222-222222222223', '11111111-1111-1111-1111-111111111111', 'admin@acme.test', 'ADMIN', '$2a$10$IDJv9Q.zcK00ENEtRJFwVeWZNTJPUfs25uMQkslSvLJ.rYOMisPem', 0)
on conflict (id) do nothing;

insert into categoria (id, organizacion_id, nombre)
values
  ('33333333-3333-3333-3333-333333333331', '11111111-1111-1111-1111-111111111111', 'Incidencia'),
  ('33333333-3333-3333-3333-333333333332', '11111111-1111-1111-1111-111111111111', 'Consulta')
on conflict (id) do nothing;

insert into secuencia_ticket (organizacion_id, ultimo)
values ('11111111-1111-1111-1111-111111111111', 0)
on conflict (organizacion_id) do nothing;
