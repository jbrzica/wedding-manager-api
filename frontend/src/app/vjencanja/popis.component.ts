import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { CurrencyPipe, DatePipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { EMPTY, Subject, catchError, debounceTime, distinctUntilChanged, switchMap } from 'rxjs';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatButtonToggleModule } from '@angular/material/button-toggle';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { ApiService } from '../core/api.service';
import { Pregled, Razdoblje, VjencanjeSazetak } from '../core/modeli';
import { izIso } from '../core/datumi';
import { porukaGreske } from '../core/greska';
import { VjencanjeRedComponent } from './vjencanje-red.component';

interface Grupa {
  mjesec: Date;
  vjencanja: VjencanjeSazetak[];
}

@Component({
  selector: 'app-popis',
  imports: [
    RouterLink, DatePipe, CurrencyPipe, MatCardModule, MatButtonModule, MatButtonToggleModule,
    MatFormFieldModule, MatInputModule, MatIconModule, MatProgressBarModule, VjencanjeRedComponent,
  ],
  template: `
    <div class="zaglavlje">
      <h1>Vjenčanja</h1>
      <a mat-flat-button routerLink="/vjencanja/novo" class="samo-desktop">
        <mat-icon>add</mat-icon> Novo vjenčanje
      </a>
    </div>

    @if (pregled(); as p) {
      <div class="sazetak">
        <mat-card appearance="outlined" class="stat">
          <span class="oznaka">Sljedeće</span>
          @if (p.sljedeceVjencanje; as s) {
            <a [routerLink]="['/vjencanja', s.id]" class="vrijednost">
              {{ p.danaDoSljedeceg === 0 ? 'danas' : 'za ' + p.danaDoSljedeceg + ' d.' }}
            </a>
            <span class="detalj">{{ s.mlada }} &amp; {{ s.mladozenja }}</span>
          } @else {
            <span class="vrijednost">—</span>
          }
        </mat-card>
        <mat-card appearance="outlined" class="stat">
          <span class="oznaka">Ove godine</span>
          <span class="vrijednost">{{ p.brojOveGodine }}</span>
          <span class="detalj">vjenčanja</span>
        </mat-card>
        <mat-card appearance="outlined" class="stat">
          <span class="oznaka">Nenaplaćeno</span>
          <span class="vrijednost">{{ p.ukupnoNenaplaceno | currency: 'EUR' : 'symbol' : '1.0-0' }}</span>
          <span class="detalj">ukupno za naplatiti</span>
        </mat-card>
      </div>
    }

    <div class="filteri">
      <mat-button-toggle-group [value]="razdoblje()" (change)="promijeniRazdoblje($event.value)" hideSingleSelectionIndicator>
        <mat-button-toggle value="NADOLAZECA">Nadolazeća</mat-button-toggle>
        <mat-button-toggle value="PROSLA">Prošla</mat-button-toggle>
        <mat-button-toggle value="SVA">Sva</mat-button-toggle>
      </mat-button-toggle-group>

      <mat-form-field appearance="outline" class="pretraga" subscriptSizing="dynamic">
        <mat-icon matPrefix>search</mat-icon>
        <input matInput placeholder="Ime, prezime, lokacija..." (input)="pretraga$.next($any($event.target).value)">
      </mat-form-field>
    </div>

    @if (ucitava()) {
      <mat-progress-bar mode="indeterminate" />
    }
    @if (greska()) {
      <p class="greska">{{ greska() }}</p>
    }

    @for (g of grupe(); track g.mjesec.getTime()) {
      <h2 class="mjesec">{{ g.mjesec | date: 'LLLL yyyy' }} <span>({{ g.vjencanja.length }})</span></h2>
      <mat-card appearance="outlined" class="lista">
        @for (v of g.vjencanja; track v.id) {
          <app-vjencanje-red [v]="v" />
        }
      </mat-card>
    } @empty {
      @if (!ucitava() && !greska()) {
        <div class="prazno">
          <mat-icon>event_busy</mat-icon>
          <p>Nema vjenčanja za prikaz.</p>
          <a mat-stroked-button routerLink="/vjencanja/novo">Dodaj prvo vjenčanje</a>
        </div>
      }
    }
  `,
  styles: `
    .zaglavlje { display: flex; align-items: center; justify-content: space-between; }
    .sazetak { display: grid; grid-template-columns: repeat(3, 1fr); gap: 12px; margin-bottom: 24px; }
    .stat { padding: 16px; display: flex; flex-direction: column; gap: 2px; }
    .oznaka { font-size: 12px; text-transform: uppercase; color: var(--mat-sys-on-surface-variant); }
    .vrijednost { font-size: 26px; font-weight: 500; color: inherit; text-decoration: none; }
    .detalj { font-size: 13px; color: var(--mat-sys-on-surface-variant); }
    .filteri { display: flex; gap: 12px; align-items: center; flex-wrap: wrap; margin-bottom: 12px; }
    .pretraga { flex: 1; min-width: 220px; }
    .mjesec { font-size: 16px; font-weight: 500; margin: 24px 0 8px; text-transform: capitalize; }
    .mjesec span { color: var(--mat-sys-on-surface-variant); font-weight: 400; }
    .lista { overflow: hidden; }
    .prazno { text-align: center; padding: 48px 0; color: var(--mat-sys-on-surface-variant); }
    .prazno mat-icon { font-size: 48px; width: 48px; height: 48px; }
    @media (max-width: 720px) {
      .sazetak { grid-template-columns: 1fr 1fr; }
      .sazetak .stat:first-child { grid-column: span 2; }
      .vrijednost { font-size: 22px; }
    }
  `,
})
export class PopisComponent implements OnInit {
  private api = inject(ApiService);

  pregled = signal<Pregled | null>(null);
  vjencanja = signal<VjencanjeSazetak[]>([]);
  razdoblje = signal<Razdoblje>('NADOLAZECA');
  ucitava = signal(true);
  greska = signal('');
  pretraga$ = new Subject<string>();
  private tekstPretrage = '';

  // Grupiramo po mjesecu: "lipanj 2026", "srpanj 2026"...
  grupe = computed<Grupa[]>(() => {
    const grupe: Grupa[] = [];
    for (const v of this.vjencanja()) {
      const d = izIso(v.datum);
      const zadnja = grupe.at(-1);
      if (zadnja && zadnja.mjesec.getFullYear() === d.getFullYear() && zadnja.mjesec.getMonth() === d.getMonth()) {
        zadnja.vjencanja.push(v);
      } else {
        grupe.push({ mjesec: new Date(d.getFullYear(), d.getMonth(), 1), vjencanja: [v] });
      }
    }
    return grupe;
  });

  constructor() {
    // Čekamo 300 ms nakon zadnjeg slova da ne šaljemo zahtjev za svako slovo
    this.pretraga$.pipe(
      debounceTime(300),
      distinctUntilChanged(),
      switchMap((tekst) => {
        this.tekstPretrage = tekst.trim();
        this.ucitava.set(true);
        // Greška ne smije ugasiti pretragu, pa je hvatamo unutar switchMap
        return this.api.popis(this.razdoblje(), this.tekstPretrage).pipe(
          catchError((err) => {
            this.prikaziGresku(err);
            return EMPTY;
          }),
        );
      }),
      takeUntilDestroyed(),
    ).subscribe((lista) => this.prikazi(lista));
  }

  ngOnInit(): void {
    this.api.pregled().subscribe((p) => this.pregled.set(p));
    this.ucitaj();
  }

  promijeniRazdoblje(r: Razdoblje): void {
    this.razdoblje.set(r);
    this.ucitaj();
  }

  private ucitaj(): void {
    this.ucitava.set(true);
    this.greska.set('');
    this.api.popis(this.razdoblje(), this.tekstPretrage).subscribe({
      next: (lista) => this.prikazi(lista),
      error: (err) => this.prikaziGresku(err),
    });
  }

  private prikazi(lista: VjencanjeSazetak[]): void {
    this.vjencanja.set(lista);
    this.ucitava.set(false);
  }

  private prikaziGresku(err: unknown): void {
    this.greska.set(porukaGreske(err));
    this.ucitava.set(false);
  }
}
