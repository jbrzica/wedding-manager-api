import { Component } from '@angular/core';
import { TvrtkaComponent } from './tvrtka.component';
import { LozinkaComponent } from './lozinka.component';

@Component({
  selector: 'app-postavke',
  imports: [TvrtkaComponent, LozinkaComponent],
  template: `
    <h1>Postavke</h1>
    <div class="kartice">
      <app-tvrtka />
      <app-lozinka />
    </div>
  `,
  styles: `
    .kartice { display: flex; flex-direction: column; gap: 16px; max-width: 720px; }
  `,
})
export class PostavkeComponent {}
