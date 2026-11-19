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
});
