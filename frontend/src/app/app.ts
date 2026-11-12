import { Component, inject } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { TemaService } from './core/tema.service';
import { VerzijaService } from './core/verzija.service';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet],
  template: '<router-outlet />',
})
export class App {
  // Pokreće se odmah, i na javnim stranicama bez prijave
  private readonly tema = inject(TemaService);
  private readonly verzija = inject(VerzijaService);
}
