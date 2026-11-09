import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { CurrencyPipe, DatePipe } from '@angular/common';
import { RouterLink } from '@angular/router';
import { EMPTY, Subject, catchError, debounceTime, distinctUntilChanged, switchMap } from 'rxjs';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatChipsModule } from '@angular/material/chips';
import { MatSelectModule } from '@angular/material/select';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatDialog } from '@angular/material/dialog';
import { ProvjeraDatumaComponent } from './provjera-datuma.component';
import { PrviKoraciComponent } from './prvi-koraci.component';
import { ApiService } from '../core/api.service';
import { FilterVjencanja, Pregled, Prognoza, Razdoblje, Stranica, StatusVjencanja, VjencanjeSazetak } from '../core/modeli';
import { STATUSI } from '../core/oznake';
import { izIso } from '../core/datumi';
import { porukaGreske } from '../core/greska';
import { VjencanjeRedComponent } from './vjencanje-red.component';

// Dovoljno za cijelu sezonu unaprijed; ostatak se učita gumbom
const PO_STRANICI = 30;

const ZADANI_FILTER: FilterVjencanja = {
  razdoblje: 'NADOLAZECA', statusi: [], godina: null, nenaplaceno: false, kasniIsporuka: false,
};
const KLJUC_FILTERA = 'wm-filter-vjencanja';

// Filter se pamti u browseru, da se ne mora birati svaki put; ako spremanje ne radi, kreće od zadanog.
function ucitajFilter(): FilterVjencanja {
  try {
    const spremljen = localStorage.getItem(KLJUC_FILTERA);
    return spremljen ? { ...ZADANI_FILTER, ...JSON.parse(spremljen) } : { ...ZADANI_FILTER };
  } catch {
    return { ...ZADANI_FILTER };
  }
}

function spremiFilter(f: FilterVjencanja): void {
  try {
    localStorage.setItem(KLJUC_FILTERA, JSON.stringify(f));
  } catch {
    // privatni prozor ili blokirana pohrana: filter jednostavno ne ostaje zapamćen
  }
}

function rasponGodina(): number[] {
  const ova = new Date().getFullYear();
  const godine: number[] = [];
  for (let g = ova + 2; g >= ova - 5; g--) {
    godine.push(g);
  }
  return godine;
}

interface Grupa {
  mjesec: Date;
  vjencanja: VjencanjeSazetak[];
}

@Component({
  selector: 'app-popis',
  imports: [
    RouterLink, DatePipe, CurrencyPipe, MatCardModule, MatButtonModule, MatChipsModule, MatSelectModule,
    MatFormFieldModule, MatInputModule, MatIconModule, MatProgressBarModule, VjencanjeRedComponent, PrviKoraciComponent,
  ],
  template: `
    <div class="zaglavlje">
      <h1>Vjenčanja</h1>
      <div class="gumbi-zaglavlja">
        <button mat-stroked-button type="button" (click)="provjeriDatum()">
          <mat-icon>event_available</mat-icon> Provjeri datum
        </button>
        <a mat-flat-button routerLink="/vjencanja/novo" class="samo-desktop">
          <mat-icon>add</mat-icon> Novo vjenčanje
        </a>
      </div>
    </div>

    <app-prvi-koraci />

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
      <mat-form-field appearance="outline" class="pretraga" subscriptSizing="dynamic">
        <mat-icon matPrefix>search</mat-icon>
        <input matInput placeholder="Ime, prezime, lokacija..." (input)="pretraga$.next($any($event.target).value)">
      </mat-form-field>
      <button mat-stroked-button type="button" class="gumb-filter" (click)="panelOtvoren.set(!panelOtvoren())"
              [attr.aria-expanded]="panelOtvoren()">
        <mat-icon>tune</mat-icon> Filteri{{ brojAktivnih() ? ' (' + brojAktivnih() + ')' : '' }}
      </button>
    </div>

    @if (panelOtvoren()) {
      <mat-card appearance="outlined" class="panel">
        <div class="sekcija">
          <span class="naslov-sekcije">Razdoblje</span>
          <mat-chip-listbox [value]="filter().razdoblje" (change)="postavi({ razdoblje: $event.value ?? 'NADOLAZECA' })" aria-label="Razdoblje">
            @for (r of razdoblja; track r.vrijednost) {
              <mat-chip-option [value]="r.vrijednost">{{ r.naziv }}</mat-chip-option>
            }
          </mat-chip-listbox>
        </div>

        <div class="sekcija">
          <span class="naslov-sekcije">Status <small>(bez odabira prikazuju se svi)</small></span>
          <mat-chip-listbox multiple [value]="filter().statusi" (change)="postavi({ statusi: $event.value })" aria-label="Status">
            @for (s of statusi; track s) {
              <mat-chip-option [value]="s">{{ oznakeStatusa[s] }}</mat-chip-option>
            }
          </mat-chip-listbox>
        </div>

        <div class="sekcija">
          <span class="naslov-sekcije">Posebno</span>
          <mat-chip-listbox multiple [value]="posebno()" (change)="postaviPosebno($event.value)" aria-label="Posebni filteri">
            <mat-chip-option value="nenaplaceno">Nije plaćeno do kraja</mat-chip-option>
            <mat-chip-option value="kasniIsporuka">Kasni isporuka</mat-chip-option>
          </mat-chip-listbox>
        </div>

        <div class="dno-panela">
          <mat-form-field appearance="outline" subscriptSizing="dynamic" class="godina">
            <mat-label>Godina</mat-label>
            <mat-select [value]="filter().godina" (selectionChange)="postavi({ godina: $event.value })">
              <mat-option [value]="null">Sve godine</mat-option>
              @for (g of godine; track g) { <mat-option [value]="g">{{ g }}</mat-option> }
            </mat-select>
          </mat-form-field>
          <span class="razmak"></span>
          <button mat-button type="button" (click)="ocisti()" [disabled]="!brojAktivnih()">Očisti</button>
          <button mat-flat-button type="button" (click)="panelOtvoren.set(false)">Gotovo</button>
        </div>
      </mat-card>
    } @else if (brojAktivnih()) {
      <mat-chip-set class="aktivni" aria-label="Aktivni filteri">
        @for (a of aktivni(); track a.kljuc) {
          <mat-chip (removed)="ukloni(a.kljuc)">
            {{ a.naziv }}
            <button matChipRemove [attr.aria-label]="'Ukloni filter ' + a.naziv"><mat-icon>cancel</mat-icon></button>
          </mat-chip>
        }
      </mat-chip-set>
    }

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
          <app-vjencanje-red [v]="v" [prognoza]="prognoze()[v.id]" />
        }
      </mat-card>
    } @empty {
      @if (!ucitava() && !greska()) {
        <div class="prazno">
          <mat-icon>event_busy</mat-icon>
          @if (brojAktivnih()) {
            <p>Nijedno vjenčanje ne odgovara filterima.</p>
            <button mat-stroked-button type="button" (click)="ocisti()">Očisti filtere</button>
          } @else {
            <p>Ovdje će biti tvoja vjenčanja, poredana po mjesecima.</p>
            <a mat-stroked-button routerLink="/vjencanja/novo">Dodaj prvo vjenčanje</a>
          }
        </div>
      }
    }

    @if (ukupno() > vjencanja().length) {
      <div class="jos">
        <span class="sivo">Prikazano {{ vjencanja().length }} od {{ ukupno() }}</span>
        <button mat-stroked-button type="button" (click)="ucitajJos()" [disabled]="ucitava()">Prikaži još</button>
      </div>
    }
  `,
  styles: `
    .jos { display: flex; flex-direction: column; align-items: center; gap: 8px; margin: 24px 0 8px; }
    .zaglavlje { display: flex; align-items: center; justify-content: space-between; gap: 12px; flex-wrap: wrap; }
    .gumbi-zaglavlja { display: flex; gap: 8px; }
    .sazetak { display: grid; grid-template-columns: repeat(3, 1fr); gap: 12px; margin-bottom: 24px; }
    .stat { padding: 16px; display: flex; flex-direction: column; gap: 2px; }
    .oznaka { font-size: 12px; text-transform: uppercase; color: var(--mat-sys-on-surface-variant); }
    .vrijednost { font-size: 26px; font-weight: 650; letter-spacing: -0.02em; color: inherit; text-decoration: none; line-height: 1.25; font-variant-numeric: tabular-nums; }
    .detalj { font-size: 13px; color: var(--mat-sys-on-surface-variant); }
    .filteri { display: flex; gap: 12px; align-items: center; margin-bottom: 12px; }
    .pretraga { flex: 1; min-width: 0; }
    .gumb-filter { height: 56px; flex-shrink: 0; }
    .panel { padding: 16px; margin-bottom: 12px; display: flex; flex-direction: column; gap: 16px; }
    .sekcija { display: flex; flex-direction: column; gap: 6px; }
    .naslov-sekcije { font-size: 13px; font-weight: 600; color: var(--mat-sys-on-surface-variant); }
    .naslov-sekcije small { font-weight: 400; }
    .dno-panela { display: flex; align-items: center; gap: 8px; flex-wrap: wrap; }
    .godina { width: 160px; }
    .razmak { flex: 1; }
    .aktivni { display: block; margin-bottom: 12px; }
    .mjesec { font-size: 16px; margin: 28px 0 10px; text-transform: capitalize; }
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
  private dialog = inject(MatDialog);
  prognoze = signal<Record<number, Prognoza>>({});

  pregled = signal<Pregled | null>(null);
  vjencanja = signal<VjencanjeSazetak[]>([]);
  ukupno = signal(0);
  private stranica = 0;
  filter = signal<FilterVjencanja>(ucitajFilter());
  panelOtvoren = signal(false);

  readonly oznakeStatusa = STATUSI;
  readonly statusi = Object.keys(STATUSI) as StatusVjencanja[];
  readonly razdoblja: { vrijednost: Razdoblje; naziv: string }[] = [
    { vrijednost: 'NADOLAZECA', naziv: 'Nadolazeća' },
    { vrijednost: 'PROSLA', naziv: 'Prošla' },
    { vrijednost: 'SVA', naziv: 'Sva' },
  ];
  readonly godine = rasponGodina();

  posebno = computed(() => {
    const f = this.filter();
    return [...(f.nenaplaceno ? ['nenaplaceno'] : []), ...(f.kasniIsporuka ? ['kasniIsporuka'] : [])];
  });

  // Ono što odstupa od zadanog prikaza (nadolazeća, svi statusi), za oznake ispod pretrage
  aktivni = computed(() => {
    const f = this.filter();
    const lista: { kljuc: string; naziv: string }[] = [];
    if (f.razdoblje !== 'NADOLAZECA') {
      lista.push({ kljuc: 'razdoblje', naziv: f.razdoblje === 'PROSLA' ? 'Prošla' : 'Sva' });
    }
    for (const s of f.statusi) {
      lista.push({ kljuc: 'status:' + s, naziv: STATUSI[s] });
    }
    if (f.godina) {
      lista.push({ kljuc: 'godina', naziv: String(f.godina) });
    }
    if (f.nenaplaceno) {
      lista.push({ kljuc: 'nenaplaceno', naziv: 'Nije plaćeno do kraja' });
    }
    if (f.kasniIsporuka) {
      lista.push({ kljuc: 'kasniIsporuka', naziv: 'Kasni isporuka' });
    }
    return lista;
  });
  brojAktivnih = computed(() => this.aktivni().length);
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
        return this.api.popis(this.filter(), this.tekstPretrage, 0, PO_STRANICI).pipe(
          catchError((err) => {
            this.prikaziGresku(err);
            return EMPTY;
          }),
        );
      }),
      takeUntilDestroyed(),
    ).subscribe((s) => this.prikazi(s, false));
  }

  ngOnInit(): void {
    this.api.pregled().subscribe((p) => this.pregled.set(p));
    // Prognoza je dodatak: ako servis ne odgovori, popis radi i bez nje
    this.api.prognoze().subscribe({ next: (p) => this.prognoze.set(p), error: () => undefined });
    this.ucitaj();
  }

  provjeriDatum(): void {
    this.dialog.open(ProvjeraDatumaComponent, { width: '480px', maxWidth: '95vw', autoFocus: false });
  }

  postavi(promjena: Partial<FilterVjencanja>): void {
    this.filter.update((f) => ({ ...f, ...promjena }));
    spremiFilter(this.filter());
    this.ucitaj();
  }

  postaviPosebno(odabrano: string[]): void {
    this.postavi({ nenaplaceno: odabrano.includes('nenaplaceno'), kasniIsporuka: odabrano.includes('kasniIsporuka') });
  }

  ukloni(kljuc: string): void {
    if (kljuc === 'razdoblje') {
      this.postavi({ razdoblje: 'NADOLAZECA' });
    } else if (kljuc.startsWith('status:')) {
      this.postavi({ statusi: this.filter().statusi.filter((s) => 'status:' + s !== kljuc) });
    } else if (kljuc === 'godina') {
      this.postavi({ godina: null });
    } else if (kljuc === 'nenaplaceno') {
      this.postavi({ nenaplaceno: false });
    } else if (kljuc === 'kasniIsporuka') {
      this.postavi({ kasniIsporuka: false });
    }
  }

  ocisti(): void {
    this.postavi(ZADANI_FILTER);
  }

  private ucitaj(): void {
    this.ucitava.set(true);
    this.greska.set('');
    this.api.popis(this.filter(), this.tekstPretrage, 0, PO_STRANICI).subscribe({
      next: (s) => this.prikazi(s, false),
      error: (err) => this.prikaziGresku(err),
    });
  }

  ucitajJos(): void {
    this.ucitava.set(true);
    this.api.popis(this.filter(), this.tekstPretrage, this.stranica + 1, PO_STRANICI).subscribe({
      next: (s) => this.prikazi(s, true),
      error: (err) => this.prikaziGresku(err),
    });
  }

  private prikazi(s: Stranica<VjencanjeSazetak>, dodaj: boolean): void {
    this.vjencanja.set(dodaj ? [...this.vjencanja(), ...s.content] : s.content);
    this.stranica = s.page.number;
    this.ukupno.set(s.page.totalElements);
    this.ucitava.set(false);
  }

  private prikaziGresku(err: unknown): void {
    this.greska.set(porukaGreske(err));
    this.ucitava.set(false);
  }
}
