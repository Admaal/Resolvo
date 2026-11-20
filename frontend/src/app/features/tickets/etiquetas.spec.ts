import { describe, expect, it } from 'vitest';
import { etiquetaEstado, etiquetaPrioridad, nivelSla, textoSla } from './etiquetas';

describe('etiquetas', () => {
  it.each([
    ['ABIERTO', 'Abierto'],
    ['PENDIENTE', 'Pendiente'],
    ['RESUELTO', 'Resuelto'],
    ['CERRADO', 'Cerrado'],
  ])('etiqueta el estado %s', (estado, texto) => {
    expect(etiquetaEstado(estado)).toBe(texto);
  });

  it.each([
    ['BAJA', 'Baja'],
    ['MEDIA', 'Media'],
    ['ALTA', 'Alta'],
    ['URGENTE', 'Urgente'],
  ])('etiqueta la prioridad %s', (prioridad, texto) => {
    expect(etiquetaPrioridad(prioridad)).toBe(texto);
  });

  it('deja intacto un valor desconocido', () => {
    expect(etiquetaEstado('OTRO')).toBe('Otro');
    expect(etiquetaPrioridad('')).toBe('');
  });

  describe('nivelSla', () => {
    const ahora = Date.parse('2026-11-20T12:00:00Z');
    const hora = 3_600_000;

    it('incumplido cuando ya venció', () => {
      expect(nivelSla({ venceEl: '2026-11-20T11:00:00Z', abiertoEn: '2026-11-20T08:00:00Z' }, ahora)).toBe('incumplido');
    });

    it('en riesgo con menos del 25 % del plazo', () => {
      const abierto = new Date(ahora - 7 * hora).toISOString();
      const vence = new Date(ahora + hora).toISOString();
      expect(nivelSla({ venceEl: vence, abiertoEn: abierto }, ahora)).toBe('riesgo');
    });

    it('en plazo con margen suficiente', () => {
      const abierto = new Date(ahora - hora).toISOString();
      const vence = new Date(ahora + 7 * hora).toISOString();
      expect(nivelSla({ venceEl: vence, abiertoEn: abierto }, ahora)).toBe('ok');
    });

    it('en plazo si no se sabe cuándo se abrió', () => {
      expect(nivelSla({ venceEl: new Date(ahora + hora).toISOString() }, ahora)).toBe('ok');
    });
  });

  it('textoSla describe cada nivel', () => {
    expect(textoSla('incumplido')).toBe('SLA incumplido');
    expect(textoSla('riesgo')).toBe('SLA en riesgo');
    expect(textoSla('ok')).toBe('SLA en plazo');
  });
});
