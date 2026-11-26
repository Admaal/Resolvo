-- Limites de uso (login, IA y comentarios) en la base en vez de en memoria: sobreviven a un
-- reinicio de la API y valen con mas de una instancia. Cada fila es una peticion contada dentro de
-- la ventana de su ambito. La clave va como SHA-256 en hex: no se guardan ni el email ni la IP.
create table limite_marca (
  ambito text not null,
  clave char(64) not null,
  en timestamptz not null
);

create index limite_marca_ambito_clave on limite_marca (ambito, clave, en);
create index limite_marca_en on limite_marca (en);
