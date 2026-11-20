import { Component } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { beforeEach, describe, expect, it, vi } from 'vitest';
import { AuthService } from './auth.service';
import { CLAVE_TEMA } from './tema';
import { Shell } from './shell';

@Component({ template: '' })
class Vacio {}

describe('Shell', () => {
  beforeEach(() => {
    localStorage.clear();
    document.documentElement.dataset['theme'] = 'dark';
    TestBed.configureTestingModule({
      imports: [Shell],
      providers: [
        provideRouter([
          { path: 'tickets', component: Vacio },
          { path: 'tickets/nuevo', component: Vacio },
        ]),
        { provide: AuthService, useValue: { salir: vi.fn() } },
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
    return Array.from(activos).map((enlace) => enlace.textContent?.trim() ?? '');
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
});
