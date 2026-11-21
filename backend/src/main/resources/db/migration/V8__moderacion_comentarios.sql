-- Moderacion de comentarios (ocultar sin borrar) y marca de los comentarios de la semilla.
-- Solo columnas nuevas que admiten nulos o llevan valor por defecto: la version anterior de la API
-- sigue funcionando contra este esquema.
alter table comentario
  add column oculto_motivo varchar(20),
  add column oculto_por uuid,
  add column oculto_en timestamptz,
  add column semilla boolean not null default false,
  add constraint comentario_oculto_motivo_valido
    check (oculto_motivo is null or oculto_motivo in ('SPAM', 'OFENSIVO', 'OTRO'));

-- Los comentarios de la demo sembrada (V3) no los borra la limpieza diaria de la demo.
update comentario
set semilla = true
where id in (
  '55555555-5555-5555-5555-555555555551',
  '55555555-5555-5555-5555-555555555552',
  '55555555-5555-5555-5555-555555555553',
  '55555555-5555-5555-5555-555555555554'
);
