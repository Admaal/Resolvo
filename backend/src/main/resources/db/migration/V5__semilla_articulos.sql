-- Articulos ficticios de ayuda de la organizacion Acme para el RAG de la fase 3.
-- Idempotente: IDs fijos y on conflict do nothing. Sin embeddings: los genera el indexador al arrancar.
insert into articulo (id, organizacion_id, titulo, cuerpo)
values
  ('66666666-6666-6666-6666-666666666661', '11111111-1111-1111-1111-111111111111',
   'Como restablecer tu contrasena',
   'En la pantalla de entrada pulsa He olvidado mi contrasena e indica tu correo. Recibiras un enlace valido durante 30 minutos. Si no llega, revisa la carpeta de spam y pide uno nuevo pasados 5 minutos.'),
  ('66666666-6666-6666-6666-666666666662', '11111111-1111-1111-1111-111111111111',
   'Errores al iniciar sesion en el panel',
   'Si el panel devuelve un error al entrar, cierra todas las pestanas, borra las cookies del sitio y vuelve a intentarlo. Tras cinco intentos fallidos seguidos la cuenta se bloquea unos minutos. Si el error persiste, abre un ticket indicando la hora exacta y el navegador.'),
  ('66666666-6666-6666-6666-666666666663', '11111111-1111-1111-1111-111111111111',
   'Exportar informes a CSV',
   'La exportacion a CSV admite hasta 10.000 filas por informe. Si tu informe es mayor, filtra por un rango de fechas mas corto y exporta por partes. Un error 500 al exportar suele indicar que se ha superado ese limite.'),
  ('66666666-6666-6666-6666-666666666664', '11111111-1111-1111-1111-111111111111',
   'Anadir usuarios a tu equipo',
   'Solo los administradores pueden invitar personas. En Equipo pulsa Invitar, escribe el correo y elige el rol: cliente, agente o administrador. La persona recibe un correo para fijar su contrasena.'),
  ('66666666-6666-6666-6666-666666666665', '11111111-1111-1111-1111-111111111111',
   'Cambiar los datos de facturacion',
   'Las facturas ya emitidas no se modifican. Para cambiar la razon social, solicita una factura rectificativa indicando el numero de factura, el nuevo nombre fiscal y el NIF. Se emite en un maximo de dos dias laborables.'),
  ('66666666-6666-6666-6666-666666666666', '11111111-1111-1111-1111-111111111111',
   'Notificaciones por correo duplicadas',
   'Recibir cada aviso dos veces suele deberse a que el correo esta dado de alta como usuario y como destinatario del equipo. En Preferencias de notificacion deja el correo en una sola de las dos listas.'),
  ('66666666-6666-6666-6666-666666666667', '11111111-1111-1111-1111-111111111111',
   'La busqueda no devuelve resultados',
   'Si el listado devuelve cero resultados, comprueba que no hay filtros activos de estado, prioridad o asignado. Pulsa Limpiar filtros y repite la busqueda. La busqueda distingue palabras completas, no fragmentos.'),
  ('66666666-6666-6666-6666-666666666668', '11111111-1111-1111-1111-111111111111',
   'Plazos de primera respuesta',
   'El plazo de primera respuesta depende de la categoria y la prioridad. Para incidencias va de 1 hora si es urgente a 24 horas si es baja. Para consultas va de 2 a 48 horas. El vencimiento aparece en cada ticket.')
on conflict (id) do nothing;
