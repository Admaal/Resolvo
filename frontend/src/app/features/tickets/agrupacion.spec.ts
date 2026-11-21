import { describe, expect, it } from 'vitest';
import { agruparPorUrgencia, ordenDeBandeja } from './agrupacion';
import { TicketResumen } from './ticket.model';

const AHORA = Date.parse('2026-11-20T12:00:00Z');
const hora = 3_600_000;

function ticket(id: string, extra: Partial<TicketResumen> = {}): TicketResumen {
  return {
    id,
    codigo: `RS-${id}`,
    asunto: `Asunto ${id}`,
    estado: 'ABIERTO',
    prioridad: 'MEDIA',
    asignadoId: null,
    autorId: 'u',
    abiertoEn: new Date(AHORA - 10 * hora).toISOString(),
    venceEl: new Date(AHORA + 30 * hora).toISOString(),
    ...extra,
  };
}

const ids = (tickets: TicketResumen[]) => tickets.map((t) => t.id);

describe('agruparPorUrgencia', () => {
  const lista = [
    ticket('resto'),
    ticket('urgente', { prioridad: 'URGENTE' }),
    ticket('riesgo', { venceEl: new Date(AHORA + hora).toISOString() }),
    ticket('incumplido', { venceEl: new Date(AHORA - hora).toISOString() }),
    ticket('resuelto-vencido', { estado: 'RESUELTO', venceEl: new Date(AHORA - hora).toISOString() }),
  ];

  it('pone primero el SLA incumplido, luego en riesgo, urgente y el resto', () => {
    const grupos = agruparPorUrgencia(lista, AHORA);

    expect(grupos.map((g) => g.clave)).toEqual(['incumplido', 'riesgo', 'urgente', 'resto']);
    expect(grupos.map((g) => ids(g.tickets))).toEqual([['incumplido'], ['riesgo'], ['urgente'], ['resto', 'resuelto-vencido']]);
  });

  it('no incluye grupos vacíos', () => {
    expect(agruparPorUrgencia([ticket('a')], AHORA).map((g) => g.clave)).toEqual(['resto']);
    expect(agruparPorUrgencia([], AHORA)).toEqual([]);
  });

  it('un ticket resuelto no cuenta como SLA incumplido', () => {
    const grupos = agruparPorUrgencia([ticket('r', { estado: 'RESUELTO', venceEl: new Date(AHORA - hora).toISOString() })], AHORA);

    expect(grupos.map((g) => g.clave)).toEqual(['resto']);
  });

  it('un urgente con SLA incumplido va solo al grupo del incumplido', () => {
    const grupos = agruparPorUrgencia([ticket('x', { prioridad: 'URGENTE', venceEl: new Date(AHORA - hora).toISOString() })], AHORA);

    expect(grupos.map((g) => g.clave)).toEqual(['incumplido']);
  });

  it('ordenDeBandeja aplana los grupos en el orden en que se ven', () => {
    expect(ids(ordenDeBandeja(lista, AHORA))).toEqual(['incumplido', 'riesgo', 'urgente', 'resto', 'resuelto-vencido']);
  });
});
