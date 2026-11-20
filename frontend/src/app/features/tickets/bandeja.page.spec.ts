import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { describe, expect, it, vi } from 'vitest';
import { AuthService } from '../../core/auth.service';
import { BandejaPage } from './bandeja.page';
import { PaginaTickets, TicketResumen } from './ticket.model';
import { TicketsService } from './tickets.service';

const ticket: TicketResumen = {
  id: 't1',
  codigo: 'RS-1',
  asunto: 'No puedo entrar',
  estado: 'ABIERTO',
  prioridad: 'ALTA',
  asignadoId: null,
  autorId: 'u1',
  venceEl: '2999-01-01T00:00:00Z',
};

function montar(listar: () => Promise<PaginaTickets>) {
  TestBed.configureTestingModule({
    imports: [BandejaPage],
    providers: [
      provideRouter([]),
      { provide: TicketsService, useValue: { listar } },
      { provide: AuthService, useValue: { usuarioId: () => 'u1' } },
    ],
  });
  const fixture = TestBed.createComponent(BandejaPage);
  fixture.detectChanges();
  return fixture;
}

const esperar = () => new Promise<void>((resolver) => setTimeout(resolver));

const el = (fixture: { nativeElement: unknown }, selector: string) => (fixture.nativeElement as HTMLElement).querySelector(selector);

describe('BandejaPage', () => {
  it('mientras carga no dice que la bandeja está vacía', () => {
    const fixture = montar(() => new Promise(() => undefined));

    expect(el(fixture, '.estado-carga')).not.toBeNull();
    expect(el(fixture, '.estado-vacio')).toBeNull();
  });

  it('avisa cuando no hay tickets', async () => {
    const fixture = montar(() => Promise.resolve({ elementos: [], pagina: 0, tamano: 20, total: 0 }));
    await esperar();
    fixture.detectChanges();

    expect(el(fixture, '.estado-vacio')).not.toBeNull();
    expect(el(fixture, '.estado-carga')).toBeNull();
  });

  it('anuncia el error si falla la carga', async () => {
    const fixture = montar(() => Promise.reject(new Error('red')));
    await esperar();
    fixture.detectChanges();

    expect(el(fixture, '.error')?.getAttribute('role')).toBe('alert');
    expect(el(fixture, '.estado-vacio')).toBeNull();
  });

  it('lista los tickets con estado legible y prioridad', async () => {
    const fixture = montar(() => Promise.resolve({ elementos: [ticket], pagina: 0, tamano: 20, total: 1 }));
    await esperar();
    fixture.detectChanges();

    expect(el(fixture, 'ul.lista-tickets li a.fila')).not.toBeNull();
    expect(el(fixture, '.chip.abierto')?.textContent?.trim()).toBe('Abierto');
    expect(el(fixture, '.chip.prioridad-alta')?.textContent?.trim()).toBe('Alta');
  });

  it('el título refleja la vista', async () => {
    const fixture = montar(() => Promise.resolve({ elementos: [], pagina: 0, tamano: 20, total: 0 }));
    await esperar();
    fixture.detectChanges();

    expect(el(fixture, 'h1')?.textContent).toContain('Todos los tickets');
  });

  describe('agrupación y búsqueda', () => {
    const hora = 3_600_000;
    const lista: TicketResumen[] = [
      { ...ticket, id: 'a', codigo: 'RS-10', asunto: 'Factura duplicada', venceEl: '2999-01-01T00:00:00Z' },
      { ...ticket, id: 'b', codigo: 'RS-11', asunto: 'No entra el login', venceEl: new Date(Date.now() - hora).toISOString() },
    ];

    async function cargada() {
      const fixture = montar(() => Promise.resolve({ elementos: lista, pagina: 0, tamano: 20, total: 2 }));
      await esperar();
      fixture.detectChanges();
      return fixture;
    }

    function buscar(fixture: { nativeElement: unknown; detectChanges(): void }, texto: string) {
      const buscador = (fixture.nativeElement as HTMLElement).querySelector('input[type="search"]') as HTMLInputElement;
      buscador.value = texto;
      buscador.dispatchEvent(new Event('input'));
      fixture.detectChanges();
    }

    it('agrupa por urgencia con el SLA incumplido primero', async () => {
      const fixture = await cargada();
      const nodo = fixture.nativeElement as HTMLElement;
      const grupos = Array.from(nodo.querySelectorAll('.grupo-titulo')).map((g) => g.textContent?.replace(/\s+/g, ' ').trim());
      const filas = Array.from(nodo.querySelectorAll('.fila .codigo')).map((c) => c.textContent?.trim());

      expect(grupos).toEqual(['SLA incumplido 1', 'Resto 1']);
      expect(filas).toEqual(['RS-11', 'RS-10']);
    });

    it('la búsqueda filtra por asunto o código', async () => {
      const fixture = await cargada();
      buscar(fixture, 'factura');

      expect((fixture.nativeElement as HTMLElement).querySelectorAll('.fila')).toHaveLength(1);

      buscar(fixture, 'rs-11');

      expect((fixture.nativeElement as HTMLElement).querySelector('.fila .codigo')?.textContent?.trim()).toBe('RS-11');
    });

    it('sin coincidencias de búsqueda lo dice y no enseña grupos', async () => {
      const fixture = await cargada();
      buscar(fixture, 'zzz');

      expect(el(fixture, '.estado-vacio')).not.toBeNull();
      expect(el(fixture, '.grupo-titulo')).toBeNull();
    });

    it('enseña el tiempo de SLA con su significado accesible', async () => {
      const fixture = await cargada();
      const sla = (fixture.nativeElement as HTMLElement).querySelector('.fila .chip.sla-incumplido') as HTMLElement;

      expect(sla.textContent).toContain('SLA incumplido');
      expect(sla.textContent).toContain('\u2212');
    });

    it('lleva un hueco para el detalle del ticket elegido', async () => {
      const fixture = await cargada();

      expect(el(fixture, 'router-outlet')).not.toBeNull();
    });

    it('un ticket resuelto no enseña chip de SLA aunque su plazo haya pasado', async () => {
      const vencido = new Date(Date.now() - 3_600_000).toISOString();
      const fixture = montar(() =>
        Promise.resolve({ elementos: [{ ...ticket, estado: 'RESUELTO', venceEl: vencido }], pagina: 0, tamano: 20, total: 1 }),
      );
      await esperar();
      fixture.detectChanges();

      expect(el(fixture, '.chip.resuelto')).not.toBeNull();
      expect(el(fixture, '[class*="sla-"]')).toBeNull();
    });
  });
});
