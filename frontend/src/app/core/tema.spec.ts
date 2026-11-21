import { describe, expect, it } from 'vitest';
import { CLAVE_TEMA, alternarTema, temaInicial } from './tema';

describe('temaInicial', () => {
  it('respeta la elección guardada', () => {
    expect(temaInicial('light', true)).toBe('light');
    expect(temaInicial('dark', false)).toBe('dark');
  });

  it('sin elección sigue la preferencia del sistema', () => {
    expect(temaInicial(null, true)).toBe('dark');
    expect(temaInicial(null, false)).toBe('light');
  });

  it('ignora un valor guardado que no es un tema', () => {
    expect(temaInicial('rosa', false)).toBe('light');
  });
});

describe('alternarTema', () => {
  it('cambia el tema del documento y lo guarda', () => {
    localStorage.clear();
    document.documentElement.dataset['theme'] = 'dark';

    expect(alternarTema()).toBe('light');
    expect(document.documentElement.dataset['theme']).toBe('light');
    expect(localStorage.getItem(CLAVE_TEMA)).toBe('light');
    expect(alternarTema()).toBe('dark');
  });
});
