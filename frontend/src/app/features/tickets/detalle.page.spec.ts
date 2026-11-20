import { HttpErrorResponse } from '@angular/common/http';
import { TestBed } from '@angular/core/testing';
import { ActivatedRoute, provideRouter } from '@angular/router';
import { describe, expect, it } from 'vitest';
import { AuthService } from '../../core/auth.service';
import { ClasificacionesService } from './clasificaciones.service';
import { DetallePage } from './detalle.page';
import { SugerenciasService } from './sugerencias.service';
import { TicketDetalle } from './ticket.model';
import { TicketsService } from './tickets.service';

const esperar = () => new Promise<void>((resolver) => setTimeout(resolver));

const ticket: TicketDetalle = {
  id: 't1',
  codigo: 'RS-1',
  asunto: 'No puedo entrar',
  estado: 'ABIERTO',
  prioridad: 'URGENTE',
  asignadoId: null,
  autorId: 'u1',
  venceEl: '2999-01-01T00:00:00Z',
  abiertoEn: '2026-11-01T00:00:00Z',
  cuerpo: 'Texto',
  categoriaId: 'c1',
  comentarios: [],
};

function montar(rol: string, detalle: () => Promise<TicketDetalle>, transicionar: () => Promise<TicketDetalle> = () => Promise.resolve(ticket)) {
  TestBed.configureTestingModule({
    imports: [DetallePage],
    providers: [
      provideRouter([]),
      { provide: ActivatedRoute, useValue: { snapshot: { paramMap: { get: () => 't1' } } } },
      { provide: AuthService, useValue: { rol: () => rol } },
      {
        provide: TicketsService,
        useValue: { detalle, transicionar, categorias: () => Promise.resolve([]), usuarios: () => Promise.resolve([]) },
      },
      { provide: ClasificacionesService, useValue: { actual: () => Promise.resolve(null) } },
      { provide: SugerenciasService, useValue: { actual: () => Promise.resolve(null) } },
    ],
  });
  const fixture = TestBed.createComponent(DetallePage);
  fixture.detectChanges();
  return fixture;
}

const el = (fixture: { nativeElement: unknown }, selector: string) => (fixture.nativeElement as HTMLElement).querySelector(selector);

describe('DetallePage', () => {
  it('mientras carga enseña un estado de carga', () => {
    const fixture = montar('CLIENTE', () => new Promise(() => undefined));

    expect(el(fixture, '.estado-carga')).not.toBeNull();
  });

  it('anuncia el error si el ticket no carga', async () => {
    const fixture = montar('CLIENTE', () => Promise.reject(new HttpErrorResponse({ status: 404 })));
    await esperar();
    fixture.detectChanges();

    expect(el(fixture, '.error')?.getAttribute('role')).toBe('alert');
    expect(el(fixture, '.estado-carga')).toBeNull();
  });

  it('enseña estado y prioridad legibles y el asunto como título', async () => {
    const fixture = montar('CLIENTE', () => Promise.resolve(ticket));
    await esperar();
    fixture.detectChanges();

    expect(el(fixture, 'h1')?.textContent).toContain('No puedo entrar');
    expect(el(fixture, '.chip.abierto')?.textContent?.trim()).toBe('Abierto');
    expect(el(fixture, '.chip.prioridad-urgente')?.textContent?.trim()).toBe('Urgente');
  });

  it('deshabilita la transición al estado actual y usa etiquetas legibles', async () => {
    const fixture = montar('AGENTE', () => Promise.resolve(ticket));
    await esperar();
    fixture.detectChanges();
    const botones = Array.from((fixture.nativeElement as HTMLElement).querySelectorAll<HTMLButtonElement>('.transiciones button'));

    expect(botones.map((b) => b.textContent?.trim())).toEqual(['Abierto', 'Pendiente', 'Resuelto', 'Cerrado']);
    expect(botones.map((b) => b.disabled)).toEqual([true, false, false, false]);
  });

  it('un conflicto no enseña el código crudo', async () => {
    const fixture = montar(
      'AGENTE',
      () => Promise.resolve(ticket),
      () => Promise.reject(new HttpErrorResponse({ status: 409, error: { error: 'conflicto' } })),
    );
    await esperar();
    fixture.detectChanges();
    await fixture.componentInstance.transicionar('CERRADO');
    fixture.detectChanges();

    expect(el(fixture, '.error')?.textContent).not.toBe('conflicto');
    expect(el(fixture, '.error')?.textContent).toContain('ha cambiado');
  });
});
