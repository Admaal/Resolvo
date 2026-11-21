-- Moderacion de comentarios (ocultar sin borrar) y marca de los comentarios de la semilla.
-- Solo columnas nuevas que admiten nulos o llevan valor por defecto: la version anterior de la API
-- sigue funcionando contra este esquema.
alter table comentario
  add column oculto_motivo varchar(20),
  add column oculto_por uuid,
  add column oculto_en timestamptz,
  add column semilla boolean not null default false,
  add constraint comentario_oculto_motivo_valido
    check (oculto_motivo is null or oculto_motivo in ('SPAM', 'OFENSIVO', 'OTRO')),
  -- La moderacion se guarda entera (motivo, quien y cuando) o no se guarda.
  add constraint comentario_moderacion_completa
    check ((oculto_motivo is null and oculto_por is null and oculto_en is null)
        or (oculto_motivo is not null and oculto_por is not null and oculto_en is not null));

-- Los comentarios de la demo sembrada (V3) no los borra la limpieza diaria de la demo.
update comentario
set semilla = true
where id in (
  '55555555-5555-5555-5555-555555555551',
  '55555555-5555-5555-5555-555555555552',
  '55555555-5555-5555-5555-555555555553',
  '55555555-5555-5555-5555-555555555554'
);
