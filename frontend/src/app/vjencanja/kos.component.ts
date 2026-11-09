import { Component, inject, OnInit, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatSnackBar } from '@angular/material/snack-bar';
import { ApiService } from '../core/api.service';
import { KosStavka } from '../core/modeli';
import { izIso } from '../core/datumi';
import { porukaGreske } from '../core/greska';

@Component({
  selector: 'app-kos',
  imports: [DatePipe, RouterLink, MatCardModule, MatButtonModule, MatIconModule, MatProgressBarModule],
  template: `
    <h1>Koš</h1>
    <p class="sivo opis">Obrisana vjenčanja čekaju ovdje 30 dana, sa svim uplatama, rasporedom i ekipom. Nakon toga se brišu zauvijek.</p>

    @if (ucitava()) {
      <mat-progress-bar mode="indeterminate" />
    } @else {
      @for (s of stavke(); track s.id) {
        <mat-card appearance="outlined" class="stavka">
          <div class="podaci">
            <strong>{{ s.mlada }} & {{ s.mladozenja }}</strong>
            <span class="sivo">
              {{ izIso(s.datum) | date: 'd. MMMM yyyy.' }}@if (s.lokacija) { · {{ s.lokacija }} }
            </span>
            <small class="sivo">Briše se zauvijek {{ s.trajnoBrisanje | date: 'd. M. yyyy.' }}</small>
          </div>
          <div class="akcije">
            <button mat-flat-button type="button" (click)="vrati(s)"><mat-icon>restore</mat-icon> Vrati</button>
            <button mat-button type="button" class="opasno" (click)="obrisiTrajno(s)">Obriši zauvijek</button>
          </div>
        </mat-card>
      } @empty {
        <mat-card appearance="outlined" class="prazno">
          <mat-icon>delete_outline</mat-icon>
          <p>Koš je prazan.</p>
          <a mat-button routerLink="/">Natrag na vjenčanja</a>
        </mat-card>
      }
    }
  `,
  styles: `
    .opis { margin: 0 0 16px; max-width: 640px; }
    .stavka { display: flex; flex-direction: row; justify-content: space-between; align-items: center;
              gap: 12px; flex-wrap: wrap; padding: 16px; margin-bottom: 12px; }
    .podaci { display: flex; flex-direction: column; gap: 2px; }
    .akcije { display: flex; gap: 4px; align-items: center; }
    .opasno { color: var(--mat-sys-error); }
    .prazno { padding: 32px; text-align: center; color: var(--mat-sys-on-surface-variant); }
    .prazno mat-icon { font-size: 40px; width: 40px; height: 40px; }
  `,
})
export class KosComponent implements OnInit {
  private api = inject(ApiService);
  private snack = inject(MatSnackBar);

  readonly izIso = izIso;
  stavke = signal<KosStavka[]>([]);
  ucitava = signal(true);

  ngOnInit(): void {
    this.ucitaj();
  }

  vrati(s: KosStavka): void {
    this.api.vratiIzKosa(s.id).subscribe({
      next: () => {
        this.poruka('Vjenčanje je vraćeno.');
        this.ucitaj();
      },
      error: (err) => this.poruka(porukaGreske(err)),
    });
  }

  obrisiTrajno(s: KosStavka): void {
    if (!confirm(`Zauvijek obrisati vjenčanje ${s.mlada} & ${s.mladozenja}? Ovo se ne može poništiti.`)) {
      return;
    }
    this.api.obrisiTrajno(s.id).subscribe({
      next: () => {
        this.poruka('Vjenčanje je obrisano zauvijek.');
        this.ucitaj();
      },
      error: (err) => this.poruka(porukaGreske(err)),
    });
  }

  private ucitaj(): void {
    this.api.kos().subscribe({
      next: (s) => {
        this.stavke.set(s);
        this.ucitava.set(false);
      },
      error: (err) => {
        this.ucitava.set(false);
        this.poruka(porukaGreske(err));
      },
    });
  }

  private poruka(tekst: string): void {
    this.snack.open(tekst, 'OK', { duration: 4000 });
  }
}
