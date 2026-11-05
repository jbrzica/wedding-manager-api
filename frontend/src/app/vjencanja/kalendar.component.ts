import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { Router } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatSelectModule } from '@angular/material/select';
import { MatFormFieldModule } from '@angular/material/form-field';
import { ApiService } from '../core/api.service';
import { VjencanjeSazetak } from '../core/modeli';
import { danas, uIso } from '../core/datumi';
import { porukaGreske } from '../core/greska';
import { VjencanjeRedComponent } from './vjencanje-red.component';

interface Dan {
  iso: string;
  broj: number;
  uMjesecu: boolean;
  vjencanja: VjencanjeSazetak[];
}

const DANI_TJEDNA = ['Pon', 'Uto', 'Sri', 'Čet', 'Pet', 'Sub', 'Ned'];

@Component({
  selector: 'app-kalendar',
  imports: [
    DatePipe, MatCardModule, MatButtonModule, MatIconModule, MatSelectModule,
    MatFormFieldModule, VjencanjeRedComponent,
  ],
  template: `
    <div class="zaglavlje">
      <button mat-icon-button (click)="pomakni(-1)" aria-label="Prethodni mjesec"><mat-icon>chevron_left</mat-icon></button>

      <!-- Brzi skok na bilo koji mjesec i godinu, npr. lipanj 2028 -->
      <mat-form-field appearance="outline" subscriptSizing="dynamic" class="izbor-mjeseca">
        <mat-select [value]="mjesec()" (selectionChange)="idiNa(godina(), $event.value)">
          @for (m of mjeseci; track $index) {
            <mat-option [value]="$index">{{ m | date: 'LLLL' }}</mat-option>
          }
        </mat-select>
      </mat-form-field>
      <mat-form-field appearance="outline" subscriptSizing="dynamic" class="izbor-godine">
        <mat-select [value]="godina()" (selectionChange)="idiNa($event.value, mjesec())">
          @for (g of godine(); track g) {
            <mat-option [value]="g">{{ g }}</mat-option>
          }
        </mat-select>
      </mat-form-field>

      <button mat-icon-button (click)="pomakni(1)" aria-label="Sljedeći mjesec"><mat-icon>chevron_right</mat-icon></button>
      <button mat-stroked-button (click)="naDanas()">Danas</button>
    </div>

    @if (greska()) {
      <p class="greska">{{ greska() }}</p>
    }

    <mat-card appearance="outlined" class="mreza">
      @for (d of daniTjedna; track d) {
        <div class="naslov-dana">{{ d }}</div>
      }
      @for (dan of dani(); track dan.iso) {
        <button class="dan"
                [class.izvan]="!dan.uMjesecu"
                [class.danas]="dan.iso === danasIso"
                [class.odabran]="dan.iso === odabrani()"
                [class.zauzet]="dan.vjencanja.length > 0"
                (click)="odabrani.set(dan.iso)">
          <span class="broj">{{ dan.broj }}</span>
          @for (v of dan.vjencanja.slice(0, 2); track v.id) {
            <span class="vjencanje" [class]="'vjencanje ' + v.status.toLowerCase()">{{ v.mlada.split(' ')[0] }} &amp; {{ v.mladozenja.split(' ')[0] }}</span>
          }
          @if (dan.vjencanja.length > 2) {
            <span class="vise">+{{ dan.vjencanja.length - 2 }}</span>
          }
          @if (dan.vjencanja.length > 0) {
            <span class="tocka">{{ dan.vjencanja.length }}</span>
          }
        </button>
      }
    </mat-card>

    @if (odabrani(); as iso) {
      <div class="odabrani-dan">
        <h2>{{ odabraniDatum() | date: 'EEEE, d. MMMM yyyy.' }}</h2>
        <button mat-stroked-button (click)="dodaj(iso)"><mat-icon>add</mat-icon> Dodaj vjenčanje</button>
      </div>
      @if (vjencanjaOdabranog().length) {
        <mat-card appearance="outlined" class="lista">
          @for (v of vjencanjaOdabranog(); track v.id) {
            <app-vjencanje-red [v]="v" />
          }
        </mat-card>
      } @else {
        <p class="slobodno">Slobodan dan.</p>
      }
    }
  `,
  styles: `
    .zaglavlje { display: flex; align-items: center; gap: 8px; flex-wrap: wrap; margin-bottom: 16px; }
    .izbor-mjeseca { flex: 1; min-width: 110px; max-width: 180px; }
    .izbor-godine { width: 100px; }
    .mreza { display: grid; grid-template-columns: repeat(7, 1fr); overflow: hidden; }
    .naslov-dana {
      padding: 8px; text-align: center; font-size: 12px; font-weight: 500;
      color: var(--mat-sys-on-surface-variant); border-bottom: 1px solid var(--mat-sys-outline-variant);
    }
    .dan {
      position: relative; min-height: 92px; padding: 6px; border: 0; background: none;
      border-right: 1px solid var(--mat-sys-outline-variant);
      border-bottom: 1px solid var(--mat-sys-outline-variant);
      display: flex; flex-direction: column; align-items: stretch; gap: 3px;
      text-align: left; cursor: pointer; font: inherit; color: inherit; overflow: hidden;
    }
    .dan:nth-child(7n) { border-right: 0; }
    .dan:hover { background: var(--mat-sys-surface-container-low); }
    .izvan { color: var(--mat-sys-outline); background: var(--mat-sys-surface-container-lowest); }
    .odabran { outline: 2px solid var(--mat-sys-primary); outline-offset: -2px; }
    .broj { font-size: 13px; width: 24px; height: 24px; line-height: 24px; text-align: center; border-radius: 50%; }
    .danas .broj { background: var(--mat-sys-primary); color: var(--mat-sys-on-primary); }
    .vjencanje {
      font-size: 11px; padding: 2px 6px; border-radius: 4px;
      white-space: nowrap; overflow: hidden; text-overflow: ellipsis;
    }
    .upit { background: #fff3cd; }
    .potvrdeno { background: #d6f0dd; }
    .odradeno { background: #dbe7fb; }
    .isporuceno { background: #e8e8e8; }
    .otkazano { background: #fde2e1; text-decoration: line-through; opacity: .7; }
    .vise { font-size: 11px; color: var(--mat-sys-on-surface-variant); }
    .tocka { display: none; }
    .odabrani-dan { display: flex; align-items: center; justify-content: space-between; gap: 8px; margin-top: 24px; flex-wrap: wrap; }
    .odabrani-dan h2 { font-size: 18px; margin: 0; }
    .lista { margin-top: 12px; overflow: hidden; }
    .slobodno { color: var(--mat-sys-on-surface-variant); }

    /* Na mobitelu nema mjesta za imena, pa prikazujemo samo broj vjenčanja */
    @media (max-width: 720px) {
      .dan { min-height: 52px; align-items: center; }
      .vjencanje, .vise { display: none; }
      .tocka {
        display: block; font-size: 10px; min-width: 16px; height: 16px; line-height: 16px;
        border-radius: 8px; text-align: center; background: var(--mat-sys-primary); color: var(--mat-sys-on-primary);
      }
    }
  `,
})
export class KalendarComponent implements OnInit {
  private api = inject(ApiService);
  private router = inject(Router);

  readonly daniTjedna = DANI_TJEDNA;
  readonly danasIso = danas();
  readonly mjeseci = Array.from({ length: 12 }, (_, i) => new Date(2000, i, 1));

  godina = signal(new Date().getFullYear());
  mjesec = signal(new Date().getMonth());
  odabrani = signal<string | null>(danas());
  vjencanja = signal<VjencanjeSazetak[]>([]);
  greska = signal('');

  // Popis godina se širi oko odabrane, pa se može ići i 10 godina unaprijed
  godine = computed(() => {
    const sredina = this.godina();
    return Array.from({ length: 21 }, (_, i) => sredina - 10 + i);
  });

  // 6 tjedana od ponedjeljka prije prvog u mjesecu
  private pocetakMreze = computed(() => {
    const prvi = new Date(this.godina(), this.mjesec(), 1);
    const pomak = (prvi.getDay() + 6) % 7;
    return new Date(this.godina(), this.mjesec(), 1 - pomak);
  });

  dani = computed<Dan[]>(() => {
    const poDatumu = new Map<string, VjencanjeSazetak[]>();
    for (const v of this.vjencanja()) {
      poDatumu.set(v.datum, [...(poDatumu.get(v.datum) ?? []), v]);
    }
    const pocetak = this.pocetakMreze();
    return Array.from({ length: 42 }, (_, i) => {
      const d = new Date(pocetak.getFullYear(), pocetak.getMonth(), pocetak.getDate() + i);
      const iso = uIso(d);
      return { iso, broj: d.getDate(), uMjesecu: d.getMonth() === this.mjesec(), vjencanja: poDatumu.get(iso) ?? [] };
    });
  });

  odabraniDatum = computed(() => {
    const iso = this.odabrani();
    if (!iso) {
      return null;
    }
    const [g, m, d] = iso.split('-').map(Number);
    return new Date(g, m - 1, d);
  });

  vjencanjaOdabranog = computed(() => this.vjencanja().filter((v) => v.datum === this.odabrani()));

  ngOnInit(): void {
    this.ucitaj();
  }

  pomakni(za: number): void {
    const d = new Date(this.godina(), this.mjesec() + za, 1);
    this.idiNa(d.getFullYear(), d.getMonth());
  }

  idiNa(godina: number, mjesec: number): void {
    this.godina.set(godina);
    this.mjesec.set(mjesec);
    this.odabrani.set(null);
    this.ucitaj();
  }

  naDanas(): void {
    const d = new Date();
    this.idiNa(d.getFullYear(), d.getMonth());
    this.odabrani.set(this.danasIso);
  }

  dodaj(iso: string): void {
    this.router.navigate(['/vjencanja/novo'], { queryParams: { datum: iso } });
  }

  private ucitaj(): void {
    const pocetak = this.pocetakMreze();
    const kraj = new Date(pocetak.getFullYear(), pocetak.getMonth(), pocetak.getDate() + 41);
    this.greska.set('');
    this.api.kalendar(uIso(pocetak), uIso(kraj)).subscribe({
      next: (lista) => this.vjencanja.set(lista),
      error: (err) => this.greska.set(porukaGreske(err)),
    });
  }
}
