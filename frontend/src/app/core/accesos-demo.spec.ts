import { TestBed } from '@angular/core/testing';
import { Router, provideRouter } from '@angular/router';
import { describe, expect, it, vi } from 'vitest';
import { AccesosDemo } from './accesos-demo';
import { AuthService } from './auth.service';

function montar(entrar: (email: string, contrasena: string) => Promise<void>) {
  TestBed.configureTestingModule({
    imports: [AccesosDemo],
    providers: [provideRouter([]), { provide: AuthService, useValue: { entrar } }],
  });
  const ir = vi.spyOn(TestBed.inject(Router), 'navigateByUrl').mockResolvedValue(true);
  const fixture = TestBed.createComponent(AccesosDemo);
  fixture.detectChanges();
  const botones = () => Array.from((fixture.nativeElement as HTMLElement).querySelectorAll<HTMLButtonElement>('button'));
  return { fixture, ir, botones };
}

describe('AccesosDemo', () => {
  it('ofrece un acceso por rol', () => {
    const { botones } = montar(() => Promise.resolve());

    expect(botones().map((b) => b.querySelector('span')?.textContent?.trim())).toEqual([
      'Entrar como Cliente',
      'Entrar como Agente',
      'Entrar como Admin',
    ]);
  });

  it.each([
    [0, 'cliente@acme.test'],
    [1, 'agente@acme.test'],
    [2, 'admin@acme.test'],
  ])('el acceso %i entra con %s y va a la bandeja', async (indice, email) => {
    const entrar = vi.fn().mockResolvedValue(undefined);
    const { fixture, ir, botones } = montar(entrar);
    botones()[indice].click();
    await fixture.whenStable();

    expect(entrar).toHaveBeenCalledWith(email, 'demo-resolvo');
    expect(ir).toHaveBeenCalledWith('/tickets');
  });

  it('deshabilita los accesos mientras entra y no entra dos veces', async () => {
    let terminar: () => void = () => undefined;
    const entrar = vi.fn().mockReturnValue(new Promise<void>((r) => (terminar = r)));
    const { fixture, botones } = montar(entrar);
    botones()[1].click();
    fixture.detectChanges();

    expect(botones().every((b) => b.disabled)).toBe(true);
    botones()[1].click();
    expect(entrar).toHaveBeenCalledTimes(1);
    terminar();
    await fixture.whenStable();
  });

  it('si falla enseña el error y vuelve a habilitar los accesos', async () => {
    const { fixture, botones } = montar(() => Promise.reject(new Error('demasiados-intentos')));
    botones()[0].click();
    await fixture.whenStable();
    fixture.detectChanges();

    expect((fixture.nativeElement as HTMLElement).querySelector('.error')?.textContent).toContain('Demasiados intentos');
    expect(botones().every((b) => !b.disabled)).toBe(true);
  });
});
