import { describe, expect, it } from 'vitest';
import { temaInicial } from './tema';

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
