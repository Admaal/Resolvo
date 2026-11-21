import { Component } from '@angular/core';
import { Icono } from '../../shared/icono';

/** Lo que se ve en el panel de detalle mientras no hay ningún ticket abierto. */
@Component({
  selector: 'app-sin-seleccion',
  imports: [Icono],
  template: `
    <div class="sin-seleccion">
      <rs-icono nombre="message-square" [tam]="48" />
      <p>Elige un ticket de la lista para ver su conversación.</p>
      <p class="ayuda">Con un ticket abierto, <kbd>J</kbd> y <kbd>K</kbd> pasan al siguiente y al anterior, y <kbd>Esc</kbd> vuelve a la lista.</p>
    </div>
  `,
})
export class SinSeleccion {}
