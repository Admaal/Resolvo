-- Tickets ficticios de la organizacion Acme para que la bandeja de la demo no salga vacia.
-- Idempotente: IDs fijos y on conflict do nothing. Las fechas son relativas al momento de aplicar la migracion.
-- Autor: cliente@acme.test (2222...221). Agente: agente@acme.test (2222...222).
-- Categorias: Incidencia 3333...331, Consulta 3333...332. Vencimientos segun la politica de SLA.
insert into ticket (id, organizacion_id, autor_id, categoria_id, prioridad, codigo, asunto, cuerpo, abierto_en, vence_el, estado, asignado_id)
values
  ('44444444-4444-4444-4444-444444444441', '11111111-1111-1111-1111-111111111111', '22222222-2222-2222-2222-222222222221', '33333333-3333-3333-3333-333333333331', 'URGENTE', 'RS-1',
   'No puedo iniciar sesion en el panel', 'Desde esta manana el panel devuelve un error al entrar con mi usuario.',
   now() - interval '20 minutes', now() - interval '20 minutes' + interval '1 hour', 'ABIERTO', null),
  ('44444444-4444-4444-4444-444444444442', '11111111-1111-1111-1111-111111111111', '22222222-2222-2222-2222-222222222221', '33333333-3333-3333-3333-333333333331', 'ALTA', 'RS-2',
   'Error 500 al exportar informes', 'Al exportar el informe mensual a CSV la aplicacion responde con un error 500.',
   now() - interval '3 hours', now() - interval '3 hours' + interval '4 hours', 'PENDIENTE', '22222222-2222-2222-2222-222222222222'),
  ('44444444-4444-4444-4444-444444444443', '11111111-1111-1111-1111-111111111111', '22222222-2222-2222-2222-222222222221', '33333333-3333-3333-3333-333333333332', 'MEDIA', 'RS-3',
   'Como anado un usuario a mi equipo', 'Necesito dar acceso a una persona nueva de mi equipo y no encuentro la opcion.',
   now() - interval '2 hours', now() - interval '2 hours' + interval '24 hours', 'ABIERTO', null),
  ('44444444-4444-4444-4444-444444444444', '11111111-1111-1111-1111-111111111111', '22222222-2222-2222-2222-222222222221', '33333333-3333-3333-3333-333333333332', 'BAJA', 'RS-4',
   'Cambio de factura a otra razon social', 'Quiero que la ultima factura salga a nombre de otra sociedad del grupo.',
   now() - interval '2 days', now() - interval '2 days' + interval '48 hours', 'RESUELTO', '22222222-2222-2222-2222-222222222222'),
  ('44444444-4444-4444-4444-444444444445', '11111111-1111-1111-1111-111111111111', '22222222-2222-2222-2222-222222222221', '33333333-3333-3333-3333-333333333331', 'MEDIA', 'RS-5',
   'Notificaciones duplicadas por correo', 'Recibo cada aviso dos veces en el correo desde la ultima actualizacion.',
   now() - interval '5 days', now() - interval '5 days' + interval '8 hours', 'CERRADO', '22222222-2222-2222-2222-222222222222'),
  ('44444444-4444-4444-4444-444444444446', '11111111-1111-1111-1111-111111111111', '22222222-2222-2222-2222-222222222221', '33333333-3333-3333-3333-333333333331', 'ALTA', 'RS-6',
   'La busqueda no devuelve resultados', 'Cualquier termino que busco en el listado da cero resultados, aunque existen.',
   now() - interval '6 hours', now() - interval '6 hours' + interval '4 hours', 'ABIERTO', null)
on conflict (id) do nothing;

insert into comentario (id, ticket_id, autor_id, cuerpo, interno, creado_en)
values
  ('55555555-5555-5555-5555-555555555551', '44444444-4444-4444-4444-444444444442', '22222222-2222-2222-2222-222222222221',
   'Ocurre solo con informes de mas de 10.000 filas.', false, now() - interval '2 hours 30 minutes'),
  ('55555555-5555-5555-5555-555555555552', '44444444-4444-4444-4444-444444444442', '22222222-2222-2222-2222-222222222222',
   'Reproducido. Parece un limite de memoria al generar el CSV, lo reviso con el equipo.', true, now() - interval '2 hours'),
  ('55555555-5555-5555-5555-555555555553', '44444444-4444-4444-4444-444444444442', '22222222-2222-2222-2222-222222222222',
   'Estamos investigando el error. Mientras tanto, prueba a exportar por rangos mas cortos de fechas.', false, now() - interval '1 hour 50 minutes'),
  ('55555555-5555-5555-5555-555555555554', '44444444-4444-4444-4444-444444444444', '22222222-2222-2222-2222-222222222222',
   'Hecho: la factura se ha reemitido a nombre de la sociedad indicada.', false, now() - interval '1 day 20 hours')
on conflict (id) do nothing;

-- La secuencia no puede quedar por debajo de los codigos sembrados.
update secuencia_ticket
set ultimo = greatest(ultimo, 6)
where organizacion_id = '11111111-1111-1111-1111-111111111111';
