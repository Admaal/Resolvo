-- El bloqueo de cuenta se retiro en la fase 6 (ADR-0013): ahora se limitan los intentos por
-- (email, cliente) en 15 minutos con 429 y Retry-After. El articulo semilla seguia diciendo lo contrario.
-- El indexador detecta el cambio por el hash del texto y regenera sus fragmentos al arrancar.
update articulo
set cuerpo = 'Si el panel devuelve un error al entrar, cierra todas las pestanas, borra las cookies del sitio y vuelve a intentarlo. Tras cinco intentos fallidos en 15 minutos el panel deja de aceptar nuevos intentos durante un rato y te indica cuanto esperar; la cuenta no se bloquea. Si el error persiste, abre un ticket indicando la hora exacta y el navegador.'
where id = '66666666-6666-6666-6666-666666666662';
