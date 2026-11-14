import { Component, inject, signal } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { ServidorService } from './core/servidor.service';

@Component({
  imports: [RouterOutlet],
  selector: 'app-root',
  styleUrl: './app.css',
  templateUrl: './app.html',
})
export class App {
  protected readonly title = signal('frontend');
  protected readonly servidor = inject(ServidorService);

  constructor() {
    void this.servidor.comprobar();
  }
}
