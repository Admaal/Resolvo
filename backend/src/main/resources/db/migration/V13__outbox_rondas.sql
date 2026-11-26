-- Rondas de reintento de un evento que se dio por FALLIDO: cada hora se reactivan los fallidos con
-- menos de tres rondas, con los intentos a cero. Pasadas tres rondas se quedan en FALLIDO.
-- Para deshacerlo: alter table evento_outbox drop column rondas;
alter table evento_outbox add column rondas int not null default 0;
