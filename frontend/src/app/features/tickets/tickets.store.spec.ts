import { TestBed } from '@angular/core/testing';
import { describe, expect, it, vi } from 'vitest';
import { TicketResumen } from './ticket.model';
import { TicketsService } from './tickets.service';
import { TicketsStore, contarPorVista, filtrarPorVista } from './tickets.store';

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
    autorId: 'otro',
    abiertoEn: new Date(AHORA - 10 * hora).toISOString(),
    venceEl: new Date(AHORA + 30 * hora).toISOString(),
    ...extra,
  };
}

const lista = [
  ticket('1', { autorId: 'yo' }),
  ticket('2', { asignadoId: 'yo' }),
  ticket('3'),
  ticket('4', { estado: 'RESUELTO', asignadoId: 'yo' }),
  ticket('5', { estado: 'CERRADO' }),
  ticket('6', { venceEl: new Date(AHORA - hora).toISOString() }),
  ticket('7', { venceEl: new Date(AHORA + hora).toISOString() }),
  ticket('8', { estado: 'RESUELTO', venceEl: new Date(AHORA - hora).toISOString() }),
];

const ids = (tickets: TicketResumen[]) => tickets.map((t) => t.id);

describe('filtrarPorVista', () => {
  it('mios: los que abrí o tengo asignados', () => {
    expect(ids(filtrarPorVista(lista, 'mios', 'yo', AHORA))).toEqual(['1', '2', '4']);
  });

  it('sin: los que no tienen asignado', () => {
    expect(ids(filtrarPorVista(lista, 'sin', 'yo', AHORA))).toEqual(['1', '3', '5', '6', '7', '8']);
  });

  it('riesgo: abiertos o pendientes con SLA en riesgo o incumplido', () => {
    expect(ids(filtrarPorVista(lista, 'riesgo', 'yo', AHORA))).toEqual(['6', '7']);
  });

  it('resueltos: resueltos y cerrados', () => {
    expect(ids(filtrarPorVista(lista, 'resueltos', 'yo', AHORA))).toEqual(['4', '5', '8']);
  });

  it('todos: sin filtro, y una vista desconocida cuenta como todos', () => {
    expect(filtrarPorVista(lista, 'todos', 'yo', AHORA)).toHaveLength(8);
    expect(filtrarPorVista(lista, 'rara', 'yo', AHORA)).toHaveLength(8);
  });
});

describe('contarPorVista', () => {
  it('cuenta cada vista con el mismo criterio que el filtro', () => {
    expect(contarPorVista(lista, 'yo', AHORA)).toEqual({ mios: 3, sin: 6, todos: 8, riesgo: 2, resueltos: 3 });
  });
});

describe('TicketsStore', () => {
  function montar(listar: (filtro: unknown, pagina: number) => Promise<unknown>) {
    TestBed.configureTestingModule({ providers: [{ provide: TicketsService, useValue: { listar } }] });
    return TestBed.inject(TicketsStore);
  }

  it('carga los tickets y pasa a listo', async () => {
    const store = montar(() => Promise.resolve({ elementos: lista, pagina: 0, tamano: 50, total: 8 }));

    expect(store.carga()).toBe('cargando');
    await store.cargar();

    expect(store.carga()).toBe('listo');
    expect(store.tickets()).toHaveLength(8);
  });

  it('carga todas las páginas y los contadores cuentan los 70', async () => {
    const todos = Array.from({ length: 70 }, (_, i) => ticket(String(i + 1)));
    const listar = vi.fn((_filtro: unknown, pagina: number) =>
      Promise.resolve({ elementos: todos.slice(pagina * 50, pagina * 50 + 50), pagina, tamano: 50, total: 70 }),
    );
    const store = montar(listar);
    await store.cargar();

    expect(listar.mock.calls.map((llamada) => llamada[1])).toEqual([0, 1]);
    expect(store.tickets()).toHaveLength(70);
    expect(store.contadores().todos).toBe(70);
  });

  it('si falla la carga queda en error y conserva lo que había', async () => {
    const listar = vi
      .fn()
      .mockResolvedValueOnce({ elementos: lista, pagina: 0, tamano: 50, total: 8 })
      .mockRejectedValueOnce(new Error('red'));
    const store = montar(listar);
    await store.cargar();
    await store.cargar();

    expect(store.carga()).toBe('error');
    expect(store.tickets()).toHaveLength(8);
  });

  it('un refresco no vuelve a cargando si ya había datos', async () => {
    let terminar: (v: unknown) => void = () => undefined;
    const listar = vi
      .fn()
      .mockResolvedValueOnce({ elementos: lista, pagina: 0, tamano: 50, total: 8 })
      .mockReturnValueOnce(new Promise((r) => (terminar = r)));
    const store = montar(listar);
    await store.cargar();
    const refresco = store.cargar();

    expect(store.carga()).toBe('listo');
    terminar({ elementos: [], pagina: 0, tamano: 50, total: 0 });
    await refresco;
    expect(store.tickets()).toEqual([]);
  });
});
