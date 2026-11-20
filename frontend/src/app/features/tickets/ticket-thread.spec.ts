import { Component, input } from '@angular/core';
import { TestBed } from '@angular/core/testing';
import { TicketThread } from './ticket-thread';
import { Comentario } from './ticket.model';

@Component({
  imports: [TicketThread],
  template: `<app-ticket-thread [comentarios]="comentarios()" />`,
})
class Host {
  readonly comentarios = input<Comentario[]>([]);
}

describe('TicketThread', () => {
  it('pinta el cuerpo como texto', async () => {
    TestBed.configureTestingModule({ imports: [Host] });
    const fixture = TestBed.createComponent(Host);
    const marca = '<script>alert(1)</script>';
    fixture.componentRef.setInput('comentarios', [
      { id: '1', autorId: 'a', cuerpo: marca, interno: false, creadoEn: '2026-11-04T10:00:00Z' },
    ]);
    fixture.detectChanges();
    await fixture.whenStable();
    const nodo = fixture.nativeElement as HTMLElement;
    expect(nodo.querySelector('script')).toBeNull();
    expect(nodo.textContent).toContain(marca);
  });

  it('no envía un borrador vacío', () => {
    TestBed.configureTestingModule({ imports: [TicketThread] });
    const fixture = TestBed.createComponent(TicketThread);
    const enviados: unknown[] = [];
    fixture.componentInstance.enviado.subscribe((evento) => enviados.push(evento));
    fixture.componentInstance.borrador = '   ';
    fixture.componentInstance.enviar(false);

    expect(enviados).toEqual([]);
  });

  it('envía el borrador recortado y lo limpia', () => {
    TestBed.configureTestingModule({ imports: [TicketThread] });
    const fixture = TestBed.createComponent(TicketThread);
    const enviados: unknown[] = [];
    fixture.componentInstance.enviado.subscribe((evento) => enviados.push(evento));
    fixture.componentInstance.borrador = ' hola ';
    fixture.componentInstance.enviar(true);

    expect(enviados).toEqual([{ cuerpo: 'hola', interno: true }]);
    expect(fixture.componentInstance.borrador).toBe('');
  });

  it('distingue la nota interna del resto', async () => {
    TestBed.configureTestingModule({ imports: [Host] });
    const fixture = TestBed.createComponent(Host);
    fixture.componentRef.setInput('comentarios', [
      { id: '1', autorId: 'a', cuerpo: 'pública', interno: false, creadoEn: '2026-11-04T10:00:00Z' },
      { id: '2', autorId: 'a', cuerpo: 'privada', interno: true, creadoEn: '2026-11-04T10:05:00Z' },
    ]);
    fixture.detectChanges();
    await fixture.whenStable();
    const nodo = fixture.nativeElement as HTMLElement;

    expect(nodo.querySelectorAll('.comentario.interno').length).toBe(1);
    expect(nodo.querySelector('.comentario.interno .chip.nota-interna')?.textContent).toContain('Nota interna');
  });
});
