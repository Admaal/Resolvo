import { HttpErrorResponse } from '@angular/common/http';
import { TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { describe, expect, it, vi } from 'vitest';
import { AltaPage } from './alta.page';
import { TicketsService } from './tickets.service';

const esperar = () => new Promise<void>((resolver) => setTimeout(resolver));

function montar(abrir: () => Promise<unknown>) {
  TestBed.configureTestingModule({
    imports: [AltaPage],
    providers: [
      provideRouter([]),
      { provide: TicketsService, useValue: { categorias: () => Promise.resolve([{ id: 'c1', nombre: 'Acceso' }]), abrir } },
    ],
  });
  const fixture = TestBed.createComponent(AltaPage);
  fixture.detectChanges();
  return fixture;
}

describe('AltaPage', () => {
  it('traduce el error en vez de enseñar el código', async () => {
    const fixture = montar(() => Promise.reject(new HttpErrorResponse({ status: 400, error: { error: 'invalido' } })));
    await esperar();
    await fixture.componentInstance.crear();
    fixture.detectChanges();
    const error = (fixture.nativeElement as HTMLElement).querySelector('.error');

    expect(error?.textContent).toContain('Revisa los datos');
    expect(error?.getAttribute('role')).toBe('alert');
  });

  it('enseña la prioridad con texto legible y mantiene el valor', async () => {
    const fixture = montar(() => Promise.resolve({ id: 't1' }));
    await esperar();
    fixture.detectChanges();
    const opciones = Array.from((fixture.nativeElement as HTMLElement).querySelectorAll<HTMLOptionElement>('select[name="prioridad"] option'));

    expect(opciones.map((o) => o.textContent?.trim())).toEqual(['Baja', 'Media', 'Alta', 'Urgente']);
    expect(opciones.map((o) => o.value)).toEqual(['BAJA', 'MEDIA', 'ALTA', 'URGENTE']);
  });

  it('deshabilita el botón mientras abre el ticket', async () => {
    let terminar: (valor: unknown) => void = () => undefined;
    const fixture = montar(() => new Promise((resolver) => (terminar = resolver)));
    await esperar();
    const abriendo = fixture.componentInstance.crear();
    fixture.detectChanges();
    const boton = (fixture.nativeElement as HTMLElement).querySelector('button[type="submit"]') as HTMLButtonElement;

    expect(boton.disabled).toBe(true);
    terminar({ id: 't1' });
    await abriendo;
  });

  it('usa .formulario y no la clase de login', () => {
    const fixture = montar(vi.fn());
    const form = (fixture.nativeElement as HTMLElement).querySelector('form');

    expect(form?.classList.contains('formulario')).toBe(true);
    expect(form?.classList.contains('login')).toBe(false);
  });
});
