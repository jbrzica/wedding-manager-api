import { Component, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { Router } from '@angular/router';
import { MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { ApiService } from '../core/api.service';
import { Dostupnost } from '../core/modeli';
import { STATUSI, ULOGE_SURADNIKA } from '../core/oznake';
import { izIso, uIso } from '../core/datumi';
import { porukaGreske } from '../core/greska';

/**
 * „Jeste li slobodni 14. lipnja?“: jedan datum i odmah se vidi je li slobodan,
 * tko iz ekipe je zauzet i ima li vjenčanja koji dan prije ili poslije.
 */
@Component({
  selector: 'app-provjera-datuma',
  imports: [DatePipe, MatDialogModule, MatButtonModule, MatIconModule, MatFormFieldModule, MatInputModule,
    MatDatepickerModule, MatProgressBarModule],
  template: `
    <h2 mat-dialog-title>Provjera datuma</h2>
    <mat-dialog-content>
      <mat-form-field appearance="outline" class="datum" subscriptSizing="dynamic">
        <mat-label>Datum</mat-label>
        <input matInput [matDatepicker]="birac" (dateChange)="provjeri($event.value)" (click)="birac.open()"
               placeholder="npr. 14.6.2027." readonly>
        <mat-datepicker-toggle matIconSuffix [for]="birac" />
        <mat-datepicker #birac />
      </mat-form-field>

      @if (ucitava()) {
        <mat-progress-bar mode="indeterminate" />
      }

      @if (rezultat(); as r) {
        <div class="ishod" [class.slobodno]="r.slobodno" [class.zauzeto]="!r.slobodno">
          <mat-icon>{{ r.slobodno ? 'event_available' : 'event_busy' }}</mat-icon>
          <div>
            <strong>{{ r.slobodno ? 'Slobodno' : 'Zauzeto' }}</strong>
            <div>{{ izIso(r.datum) | date: 'EEEE, d. MMMM yyyy.' }}</div>
          </div>
        </div>

        @if (r.vjencanja.length) {
          <h3>Taj dan</h3>
          @for (v of r.vjencanja; track v.id) {
            <button type="button" class="red" (click)="otvori(v.id)">
              <span>{{ v.mlada }} & {{ v.mladozenja }}</span>
              <span class="sivo">{{ statusi[v.status] }}</span>
            </button>
          }
        }

        @if (r.suradnici.length) {
          <h3>Ekipa taj dan</h3>
          @for (s of r.suradnici; track s.id) {
            <div class="suradnik">
              <span>{{ s.ime }}@if (s.uloga) { <span class="sivo"> · {{ uloge[s.uloga] }}</span> }</span>
              @if (s.naVjencanju) {
                <span class="oznaka zauzet">Zauzet ({{ s.naVjencanju }})</span>
              } @else {
                <span class="oznaka slobodan">Slobodan</span>
              }
            </div>
          }
        }

        @if (r.blizu.length) {
          <h3>Koji dan prije ili poslije</h3>
          @for (v of r.blizu; track v.id) {
            <button type="button" class="red" (click)="otvori(v.id)">
              <span>{{ izIso(v.datum) | date: 'EEE d. M.' }} · {{ v.mlada }} & {{ v.mladozenja }}</span>
              <span class="sivo">{{ statusi[v.status] }}</span>
            </button>
          }
        }
      }
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-button mat-dialog-close>Zatvori</button>
    </mat-dialog-actions>
  `,
  styles: `
    .datum { width: 100%; margin-top: 4px; }
    .ishod { display: flex; gap: 12px; align-items: center; padding: 12px 14px; border-radius: 12px; margin: 12px 0 4px; }
    .ishod mat-icon { font-size: 32px; width: 32px; height: 32px; }
    .ishod strong { font-size: 17px; }
    .slobodno { background: var(--wm-uspjeh-pozadina); color: var(--wm-uspjeh); }
    .zauzeto { background: var(--wm-opasno-pozadina); color: var(--wm-opasno); }
    h3 { font-size: 13px; font-weight: 600; text-transform: uppercase; letter-spacing: .04em;
         color: var(--mat-sys-on-surface-variant); margin: 18px 0 6px; }
    .red { display: flex; justify-content: space-between; gap: 12px; width: 100%; padding: 10px 0; border: none;
           background: none; border-top: 1px solid var(--mat-sys-outline-variant); font: inherit; color: inherit;
           text-align: left; cursor: pointer; }
    .red:hover { color: var(--mat-sys-primary); }
    .suradnik { display: flex; justify-content: space-between; align-items: center; gap: 12px; flex-wrap: wrap; padding: 6px 0; }
    .oznaka { font-size: 12px; padding: 2px 8px; border-radius: 999px; }
    .zauzet { background: var(--wm-opasno-pozadina); color: var(--wm-opasno); }
    .slobodan { background: var(--wm-uspjeh-pozadina); color: var(--wm-uspjeh); }
  `,
})
export class ProvjeraDatumaComponent {
  private api = inject(ApiService);
  private router = inject(Router);
  private dialogRef = inject(MatDialogRef<ProvjeraDatumaComponent>);

  readonly izIso = izIso;
  readonly statusi = STATUSI;
  readonly uloge = ULOGE_SURADNIKA;
  rezultat = signal<Dostupnost | null>(null);
  ucitava = signal(false);
  greska = signal<string | null>(null);

  provjeri(datum: Date | null): void {
    if (!datum) {
      return;
    }
    this.ucitava.set(true);
    this.api.dostupnost(uIso(datum)).subscribe({
      next: (r) => {
        this.rezultat.set(r);
        this.ucitava.set(false);
      },
      error: (err) => {
        this.greska.set(porukaGreske(err));
        this.ucitava.set(false);
      },
    });
  }

  otvori(id: number): void {
    this.dialogRef.close();
    this.router.navigate(['/vjencanja', id]);
  }
}
