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
});
