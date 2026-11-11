import { Component } from '@angular/core';
import { TvrtkaComponent } from './tvrtka.component';
import { LozinkaComponent } from './lozinka.component';
import { KalendarLinkComponent } from './kalendar.component';
import { IzgledComponent } from './izgled.component';
import { UgovorPredlozakComponent } from './ugovor-predlozak.component';
import { PovijestTvrtkeComponent } from '../povijest/povijest-tvrtke.component';

@Component({
  selector: 'app-postavke',
  imports: [TvrtkaComponent, UgovorPredlozakComponent, IzgledComponent, KalendarLinkComponent, LozinkaComponent, PovijestTvrtkeComponent],
  template: `
    <h1>Postavke</h1>
    <div class="kartice">
      <app-tvrtka />
      <app-ugovor-predlozak />
      <app-izgled />
      <app-kalendar-link />
      <app-lozinka />
      <app-povijest-tvrtke [sazeto]="true" />
    </div>
  `,
  styles: `
    .kartice { display: flex; flex-direction: column; gap: 16px; max-width: 720px; }
  `,
})
export class PostavkeComponent {}
