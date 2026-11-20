import { HttpErrorResponse } from '@angular/common/http';
import { signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { ActivatedRoute, Router, convertToParamMap, provideRouter } from '@angular/router';
import { of } from 'rxjs';
import { describe, expect, it, vi } from 'vitest';
import { AuthService } from '../../core/auth.service';
import { ClasificacionesService } from './clasificaciones.service';
import { DetallePage } from './detalle.page';
import { SugerenciasService } from './sugerencias.service';
import { TicketDetalle } from './ticket.model';
import { TicketsService } from './tickets.service';
import { TicketsStore } from './tickets.store';

const resumen = (id: string, codigo: string) => ({
  id,
  codigo,
  asunto: `Asunto ${codigo}`,
  estado: 'ABIERTO',
  prioridad: 'MEDIA',
  asignadoId: null,
  autorId: 'u1',
  abiertoEn: '2026-11-01T00:00:00Z',
  venceEl: '2999-01-01T00:00:00Z',
});
const lista = [resumen('t0', 'RS-0'), resumen('t1', 'RS-1'), resumen('t2', 'RS-2')];

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

function montar(rol: string, detalle: () => Promise<TicketDetalle>, transicionar: () => Promise<TicketDetalle> = () => Promise.resolve(ticket), vista = '') {
  TestBed.configureTestingModule({
    imports: [DetallePage],
    providers: [
      provideRouter([]),
      { provide: ActivatedRoute, useValue: { paramMap: of(convertToParamMap({ id: 't1' })), queryParamMap: of(convertToParamMap(vista ? { vista } : {})) } },
      { provide: TicketsStore, useValue: { tickets: signal(lista), cargar: vi.fn() } },
      { provide: AuthService, useValue: { rol: () => rol, usuarioId: () => 'u1' } },
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

  it('volver lleva a la bandeja con la vista actual', async () => {
    const fixture = montar('AGENTE', () => Promise.resolve(ticket), undefined, 'mios');
    await esperar();
    fixture.detectChanges();

    expect(el(fixture, 'a.volver')?.getAttribute('href')).toBe('/tickets?vista=mios');
  });

  it('enseña la posición y enlaza al anterior y al siguiente', async () => {
    const fixture = montar('AGENTE', () => Promise.resolve(ticket));
    await esperar();
    fixture.detectChanges();

    expect(el(fixture, '.posicion')?.textContent?.trim()).toBe('2 / 3');
    expect(el(fixture, 'a.anterior')?.getAttribute('href')).toBe('/tickets/t0');
    expect(el(fixture, 'a.siguiente')?.getAttribute('href')).toBe('/tickets/t2');
  });

  it('en el primero no hay anterior', async () => {
    const fixture = montar('AGENTE', () => Promise.resolve({ ...ticket, id: 't0', codigo: 'RS-0' }));
    await esperar();
    fixture.detectChanges();

    expect(el(fixture, 'a.anterior')).toBeNull();
    expect(el(fixture, 'a.siguiente')?.getAttribute('href')).toBe('/tickets/t1');
  });

  it('la tecla j abre el siguiente y Escape vuelve a la bandeja', async () => {
    const fixture = montar('AGENTE', () => Promise.resolve(ticket));
    const ir = vi.spyOn(TestBed.inject(Router), 'navigateByUrl').mockResolvedValue(true);
    await esperar();
    fixture.detectChanges();
    document.dispatchEvent(new KeyboardEvent('keydown', { key: 'j' }));
    document.dispatchEvent(new KeyboardEvent('keydown', { key: 'Escape' }));

    expect(ir.mock.calls.map((c) => String(c[0]))).toEqual(['/tickets/t2', '/tickets']);
  });

  it('las teclas no actúan mientras se escribe en un campo', async () => {
    const fixture = montar('AGENTE', () => Promise.resolve(ticket));
    const ir = vi.spyOn(TestBed.inject(Router), 'navigateByUrl').mockResolvedValue(true);
    await esperar();
    fixture.detectChanges();
    const campo = document.createElement('textarea');
    document.body.appendChild(campo);
    campo.dispatchEvent(new KeyboardEvent('keydown', { key: 'j', bubbles: true }));
    campo.remove();

    expect(ir).not.toHaveBeenCalled();
  });

  it('muestra las propiedades en una columna lateral con la fecha de vencimiento', async () => {
    const fixture = montar('AGENTE', () => Promise.resolve(ticket));
    await esperar();
    fixture.detectChanges();

    expect(el(fixture, '.propiedades')).not.toBeNull();
    expect(el(fixture, '.propiedades time')).not.toBeNull();
  });
});
