import { Component, effect, inject, input, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { ApiService } from '../core/api.service';
import { Promjena, VjencanjeDetalji } from '../core/modeli';

const PRIKAZI = 5;

// Kartica „Povijest“ na vjenčanju; ponovno se učita svaki put kad se vjenčanje osvježi
@Component({
  selector: 'app-povijest-vjencanja',
  imports: [DatePipe, MatCardModule, MatButtonModule],
  template: `
    <mat-card appearance="outlined">
      <mat-card-header><mat-card-title>Povijest</mat-card-title></mat-card-header>
      <mat-card-content>
        @for (p of (sve() ? promjene() : promjene().slice(0, prikazi)); track p.id) {
          <div class="zapis">
            <span class="vrijeme">{{ p.vrijeme | date: 'd. M. HH:mm' }}</span>
            <span><strong>{{ p.korisnik }}</strong> · {{ p.opis }}</span>
          </div>
        } @empty {
          <p class="sivo">Još nema zabilježenih promjena.</p>
        }
        @if (!sve() && promjene().length > prikazi) {
          <button mat-button type="button" (click)="sve.set(true)">Prikaži sve ({{ promjene().length }})</button>
        }
      </mat-card-content>
    </mat-card>
  `,
  styles: `
    mat-card-content { padding-top: 8px; }
    .zapis { display: flex; gap: 12px; padding: 6px 0; font-size: 14px; border-bottom: 1px solid var(--mat-sys-outline-variant); }
    .zapis:last-of-type { border-bottom: none; }
    .vrijeme { color: var(--mat-sys-on-surface-variant); white-space: nowrap; font-variant-numeric: tabular-nums; min-width: 82px; }
  `,
})
export class PovijestVjencanjaComponent {
  private api = inject(ApiService);

  vjencanje = input.required<VjencanjeDetalji>();
  promjene = signal<Promjena[]>([]);
  sve = signal(false);
  readonly prikazi = PRIKAZI;

  constructor() {
    effect(() => {
      const id = this.vjencanje().id;
      this.api.povijestVjencanja(id).subscribe({ next: (p) => this.promjene.set(p), error: () => this.promjene.set([]) });
    });
  }
}
