import { Component } from '@angular/core';
import { TvrtkaComponent } from './tvrtka.component';
import { LozinkaComponent } from './lozinka.component';
import { KalendarLinkComponent } from './kalendar.component';
import { PovijestTvrtkeComponent } from '../povijest/povijest-tvrtke.component';

@Component({
  selector: 'app-postavke',
  imports: [TvrtkaComponent, KalendarLinkComponent, LozinkaComponent, PovijestTvrtkeComponent],
  template: `
    <h1>Postavke</h1>
    <div class="kartice">
      <app-tvrtka />
      <app-kalendar-link />
      <app-lozinka />
      <app-povijest-tvrtke />
    </div>
  `,
  styles: `
    .kartice { display: flex; flex-direction: column; gap: 16px; max-width: 720px; }
  `,
})
export class PostavkeComponent {}
