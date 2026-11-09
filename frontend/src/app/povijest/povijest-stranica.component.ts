import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { PovijestTvrtkeComponent } from './povijest-tvrtke.component';

@Component({
  selector: 'app-povijest-stranica',
  imports: [RouterLink, MatButtonModule, MatIconModule, PovijestTvrtkeComponent],
  template: `
    <div class="zaglavlje">
      <a mat-icon-button routerLink="/postavke" aria-label="Natrag na postavke"><mat-icon>arrow_back</mat-icon></a>
      <h1>Povijest promjena</h1>
    </div>
    <app-povijest-tvrtke />
  `,
  styles: `
    :host { display: block; max-width: 720px; }
    .zaglavlje { display: flex; align-items: center; gap: 8px; }
    h1 { margin: 0; }
    app-povijest-tvrtke { display: block; margin-top: 16px; }
  `,
})
export class PovijestStranicaComponent {}
