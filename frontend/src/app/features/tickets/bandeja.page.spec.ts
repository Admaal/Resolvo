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
});
