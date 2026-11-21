import { Component, signal } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { AuthService } from './auth.service';
import { CLAVE_TEMA } from './tema';
import { Shell } from './shell';
import { TicketsStore } from '../features/tickets/tickets.store';

@Component({ template: '' })
class Vacio {}

const CONTADORES = { mios: 2, sin: 1, todos: 5, riesgo: 3, resueltos: 0 };

describe('Shell', () => {
  function configurar(rol: string) {
    TestBed.resetTestingModule();
    TestBed.configureTestingModule({
      imports: [Shell],
      providers: [
        provideRouter([
          { path: 'tickets', component: Vacio },
          { path: 'tickets/nuevo', component: Vacio },
          { path: 'tickets/:id', component: Vacio },
        ]),
        { provide: AuthService, useValue: { salir: vi.fn(), rol: () => rol, usuarioId: () => 'u1' } },
        { provide: TicketsStore, useValue: { contadores: signal(CONTADORES), cargar: vi.fn() } },
      ],
    });
  }

  beforeEach(() => {
    localStorage.clear();
    document.documentElement.dataset['theme'] = 'dark';
    TestBed.configureTestingModule({
      imports: [Shell],
      providers: [
        provideRouter([
          { path: 'tickets', component: Vacio },
          { path: 'tickets/nuevo', component: Vacio },
          { path: 'tickets/:id', component: Vacio },
        ]),
        { provide: AuthService, useValue: { salir: vi.fn(), rol: () => 'AGENTE', usuarioId: () => 'u1' } },
        { provide: TicketsStore, useValue: { contadores: signal(CONTADORES), cargar: vi.fn() } },
      ],
    });
  });

  async function actual(url: string): Promise<string[]> {
    const fixture = TestBed.createComponent(Shell);
    await TestBed.inject(Router).navigateByUrl(url);
    fixture.detectChanges();
    await fixture.whenStable();
    fixture.detectChanges();
    const activos = (fixture.nativeElement as HTMLElement).querySelectorAll('a[aria-current="page"]');
    return Array.from(activos).map((enlace) => (enlace.querySelector('.nombre') ?? enlace).textContent?.trim() ?? '');
  }

  it('marca Mis tickets como página actual', async () => {
    expect(await actual('/tickets?vista=mios')).toEqual(['Mis tickets']);
  });

  it('marca Todos cuando no hay vista elegida', async () => {
    expect(await actual('/tickets')).toEqual(['Todos']);
  });

  it('marca Nuevo ticket', async () => {
    expect(await actual('/tickets/nuevo')).toEqual(['Nuevo ticket']);
  });

  it('guarda el tema elegido', () => {
    const fixture = TestBed.createComponent(Shell);
    fixture.detectChanges();
    (fixture.nativeElement as HTMLElement).querySelector<HTMLButtonElement>('.tema')?.click();

    expect(localStorage.getItem(CLAVE_TEMA)).toBe('light');
    expect(document.documentElement.dataset['theme']).toBe('light');
  });

  it('marca la vista también al ver un ticket de esa vista', async () => {
    expect(await actual('/tickets/abc?vista=riesgo')).toEqual(['SLA en riesgo']);
  });

  it('muestra el contador de cada vista', () => {
    const fixture = TestBed.createComponent(Shell);
    fixture.detectChanges();
    const contadores = Array.from((fixture.nativeElement as HTMLElement).querySelectorAll('.nav a')).map(
      (a) => `${a.querySelector('.nombre')?.textContent?.trim()}:${a.querySelector('.contador')?.textContent?.trim() ?? ''}`,
    );

    expect(contadores).toEqual(['Mis tickets:2', 'Sin asignar:1', 'Todos:5', 'SLA en riesgo:3', 'Resueltos:']);
  });

  it('el cliente no ve Sin asignar ni SLA en riesgo', () => {
    configurar('CLIENTE');
    const fixture = TestBed.createComponent(Shell);
    fixture.detectChanges();
    const nombres = Array.from((fixture.nativeElement as HTMLElement).querySelectorAll('.nav .nombre')).map((n) => n.textContent?.trim());

    expect(nombres).toEqual(['Mis tickets', 'Todos', 'Resueltos']);
  });

  it('enseña el rol de la persona', () => {
    const fixture = TestBed.createComponent(Shell);
    fixture.detectChanges();

    expect((fixture.nativeElement as HTMLElement).querySelector('.rol')?.textContent?.trim()).toBe('Agente');
  });
});
