-- El bloqueo de cuenta se retiro (ADR-0013): con cuentas demo publicas cualquiera podia bloquear a
-- todos. Estas dos columnas no las lee ni escribe nadie desde entonces.
--
-- Para deshacerlo (Flyway Community no tiene "undo"; se aplicaria a mano):
--   alter table usuario add column fallos_seguidos integer not null default 0;
--   alter table usuario add column bloqueado_hasta timestamptz;
alter table usuario drop column fallos_seguidos;
alter table usuario drop column bloqueado_hasta;
