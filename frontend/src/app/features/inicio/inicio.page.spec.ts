import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { describe, expect, it, vi } from 'vitest';
import { AuthService } from '../../core/auth.service';
import { InicioPage } from './inicio.page';

function montar(rol: string | null) {
  TestBed.configureTestingModule({
    imports: [InicioPage],
    providers: [provideRouter([]), { provide: AuthService, useValue: { rol: () => rol, entrar: vi.fn() } }],
  });
  const fixture = TestBed.createComponent(InicioPage);
  fixture.detectChanges();
  return fixture.nativeElement as HTMLElement;
}

describe('InicioPage', () => {
  it('tiene un titular como único h1', () => {
    const nodo = montar(null);

    expect(nodo.querySelectorAll('h1')).toHaveLength(1);
    expect(nodo.querySelector('h1')?.textContent?.length).toBeGreaterThan(10);
  });

  it('sin sesión ofrece entrar como Cliente, Agente y Admin', () => {
    const nodo = montar(null);
    const botones = Array.from(nodo.querySelectorAll('app-accesos-demo button span')).map((s) => s.textContent?.trim());

    expect(botones).toEqual(['Entrar como Cliente', 'Entrar como Agente', 'Entrar como Admin']);
  });

  it('sin sesión enlaza a iniciar sesión', () => {
    const enlace = montar(null).querySelector('a[href="/login"]');

    expect(enlace?.textContent).toContain('Iniciar sesión');
  });

  it('con sesión abierta enlaza a la bandeja y no repite los accesos demo', () => {
    const nodo = montar('AGENTE');
    const enlace = nodo.querySelector('a[href="/tickets"]');

    expect(enlace?.textContent).toContain('Ir a la bandeja');
    expect(nodo.querySelector('app-accesos-demo')).toBeNull();
  });

  it('explica el valor del producto con citas, SLA y aprobación humana', () => {
    const texto = montar(null).textContent ?? '';

    expect(texto).toContain('citas');
    expect(texto).toContain('SLA');
    expect(texto).toContain('apruebas');
  });
});
