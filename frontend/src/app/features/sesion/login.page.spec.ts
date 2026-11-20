import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
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
});
