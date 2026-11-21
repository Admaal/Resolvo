import { HttpErrorResponse } from '@angular/common/http';
import { describe, expect, it } from 'vitest';
import { mensajeDeError } from './errores';

function http(codigo: string | undefined, status = 400): HttpErrorResponse {
  return new HttpErrorResponse({ status, error: codigo === undefined ? null : { error: codigo } });
}

describe('mensajeDeError', () => {
  it.each([
    ['conflicto', 'ha cambiado'],
    ['invalido', 'Revisa los datos'],
    ['prohibido', 'permiso'],
    ['demasiados-enlaces', 'enlaces'],
    ['demasiadas-peticiones', 'Espera'],
  ])('traduce el código %s', (codigo, texto) => {
    expect(mensajeDeError(http(codigo))).toContain(texto);
  });

  it('usa un texto genérico para un código desconocido', () => {
    expect(mensajeDeError(http('raro'))).toBe('No se pudo completar la acción.');
  });

  it('usa un texto genérico si no es una respuesta HTTP', () => {
    expect(mensajeDeError(new Error('x'))).toBe('No se pudo completar la acción.');
  });

  it('nunca devuelve el código crudo', () => {
    expect(mensajeDeError(http('conflicto'))).not.toBe('conflicto');
  });
});
