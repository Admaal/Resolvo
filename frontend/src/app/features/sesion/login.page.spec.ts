import { TestBed } from '@angular/core/testing';
import { ActivatedRoute, Router, provideRouter } from '@angular/router';
import { describe, expect, it, vi } from 'vitest';
import { AuthService } from '../../core/auth.service';
import { LoginPage } from './login.page';

describe('LoginPage', () => {
  function montar(fallo: Error) {
    TestBed.configureTestingModule({
      imports: [LoginPage],
      providers: [provideRouter([]), { provide: AuthService, useValue: { entrar: vi.fn().mockRejectedValue(fallo) } }],
    });
    return TestBed.createComponent(LoginPage);
  }

  it.each([
    ['demasiados-intentos', 'Demasiados intentos'],
    ['no-autorizado', 'Email o contraseña incorrectos'],
    ['otra-cosa', 'No se pudo entrar'],
  ])('traduce el codigo %s', async (codigo, texto) => {
    const fixture = montar(new Error(codigo));
    await fixture.componentInstance.entrar();
    fixture.detectChanges();

    expect(fixture.nativeElement.querySelector('.error')?.textContent).toContain(texto);
  });

  it('usa un campo de email y anuncia el error', async () => {
    const fixture = montar(new Error('no-autorizado'));
    await fixture.componentInstance.entrar();
    fixture.detectChanges();
    const nodo = fixture.nativeElement as HTMLElement;

    expect(nodo.querySelector('input[name="email"]')?.getAttribute('type')).toBe('email');
    expect(nodo.querySelector('.error')?.getAttribute('role')).toBe('alert');
  });

  it('deshabilita el botón mientras entra', async () => {
    let terminar: () => void = () => undefined;
    const pendiente = new Promise<void>((resolver) => (terminar = resolver));
    TestBed.configureTestingModule({
      imports: [LoginPage],
      providers: [provideRouter([]), { provide: AuthService, useValue: { entrar: vi.fn().mockReturnValue(pendiente) } }],
    });
    const fixture = TestBed.createComponent(LoginPage);
    fixture.detectChanges();
    const entrando = fixture.componentInstance.entrar();
    fixture.detectChanges();
    const boton = (fixture.nativeElement as HTMLElement).querySelector('button[type="submit"]') as HTMLButtonElement;

    expect(boton.disabled).toBe(true);
    expect(boton.textContent).toContain('Entrando');
    terminar();
    await entrando;
  });

  describe('volver', () => {
    function conVolver(volver: string | null) {
      TestBed.configureTestingModule({
        imports: [LoginPage],
        providers: [
          provideRouter([]),
          { provide: AuthService, useValue: { entrar: vi.fn().mockResolvedValue(undefined) } },
          { provide: ActivatedRoute, useValue: { snapshot: { queryParamMap: { get: () => volver } } } },
        ],
      });
      const ir = vi.spyOn(TestBed.inject(Router), 'navigateByUrl').mockResolvedValue(true);
      return { fixture: TestBed.createComponent(LoginPage), ir };
    }

    it('tras entrar vuelve a la ruta pedida', async () => {
      const { fixture, ir } = conVolver('/tickets/abc?vista=mios');
      await fixture.componentInstance.entrar();

      expect(ir).toHaveBeenCalledWith('/tickets/abc?vista=mios');
    });

    it.each(['https://malo.example/', '//malo.example', 'tickets'])('ignora un volver externo o relativo: %s', async (volver) => {
      const { fixture, ir } = conVolver(volver);
      await fixture.componentInstance.entrar();

      expect(ir).toHaveBeenCalledWith('/tickets');
    });
  });
});
