import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { CurrencyPipe, DatePipe, DecimalPipe } from '@angular/common';
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
import { PrviKoraciComponent } from './prvi-koraci.component';
import { ApiService } from '../core/api.service';
import { FilterVjencanja, Pregled, Prognoza, Stranica, StatusVjencanja, VjencanjeSazetak } from '../core/modeli';
import { STATUSI } from '../core/oznake';
import { izIso } from '../core/datumi';
import { par, zaKoliko } from '../core/imena';
import { opisVremena } from '../core/vrijeme';
import { porukaGreske } from '../core/greska';
import { VjencanjeRedComponent } from './vjencanje-red.component';

// Dovoljno za cijelu sezonu unaprijed; ostatak se učita gumbom
const PO_STRANICI = 30;

// Tri pogleda na isti popis: što dolazi, što još treba predati i sve
type Kartica = 'NADOLAZECA' | 'ISPORUKA' | 'SVA';

interface OdabirFiltera {
  kartica: Kartica;
  statusi: StatusVjencanja[];
  godina: number | null;
  nenaplaceno: boolean;
  kasniIsporuka: boolean;
  bezKapare: boolean;
}

const ZADANI_FILTER: OdabirFiltera = {
  kartica: 'NADOLAZECA', statusi: [], godina: null, nenaplaceno: false, kasniIsporuka: false, bezKapare: false,
};
// Novi ključ: stari zapis ima drugačija polja
const KLJUC_FILTERA = 'wm-filter-popisa';

// Filter se pamti u browseru, da se ne mora birati svaki put; ako spremanje ne radi, kreće od zadanog.
function ucitajFilter(): OdabirFiltera {
  try {
    const spremljen = localStorage.getItem(KLJUC_FILTERA);
    return spremljen ? { ...ZADANI_FILTER, ...JSON.parse(spremljen) } : { ...ZADANI_FILTER };
  } catch {
    return { ...ZADANI_FILTER };
  }
}

function spremiFilter(f: OdabirFiltera): void {
  try {
    localStorage.setItem(KLJUC_FILTERA, JSON.stringify(f));
  } catch {
    // privatni prozor ili blokirana pohrana: filter jednostavno ne ostaje zapamćen
  }
}

// Što se šalje backendu. "Za isporuku" su prošla vjenčanja koja još nisu označena kao isporučena.
function zaBackend(f: OdabirFiltera): FilterVjencanja {
  const zajednicko = { godina: f.godina, nenaplaceno: f.nenaplaceno, kasniIsporuka: f.kasniIsporuka, bezKapare: f.bezKapare };
  if (f.kartica === 'ISPORUKA') {
    return { ...zajednicko, razdoblje: 'PROSLA', statusi: ['POTVRDENO', 'ODRADENO'] };
  }
  return { ...zajednicko, razdoblje: f.kartica, statusi: f.statusi };
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
    RouterLink, DatePipe, CurrencyPipe, DecimalPipe, MatCardModule, MatButtonModule, MatChipsModule, MatSelectModule,
    MatFormFieldModule, MatInputModule, MatIconModule, MatProgressBarModule, VjencanjeRedComponent, PrviKoraciComponent,
  ],
  template: `
    <app-prvi-koraci />

    @if (pregled(); as p) {
      @if (p.sljedeceVjencanje; as s) {
        <a class="sljedece" [routerLink]="['/vjencanja', s.id]">
          <span class="nadnaslov">Sljedeće · {{ zaKoliko(p.danaDoSljedeceg ?? 0) }}</span>
          <span class="par">{{ par(s.mlada, s.mladozenja) }}</span>
          <span class="gdje">
            {{ izIso(s.datum) | date: 'EEEE, d. MMMM' }}@if (s.lokacija) { · {{ s.lokacija }} }
          </span>
          <span class="oznake">
            @if (p.pocetakSljedeceg) {
              <span class="oznaka"><mat-icon inline>schedule</mat-icon> početak {{ p.pocetakSljedeceg.slice(0, 5) }}</span>
            }
            @if (prognoze()[s.id]; as vr) {
              <span class="oznaka">{{ vrijeme(vr.kod).ikona }} {{ vr.max | number: '1.0-0' }}°</span>
            }
            <span class="oznaka" [class.upozori]="!p.ekipaSljedeceg">
              <mat-icon inline>group</mat-icon> {{ p.ekipaSljedeceg ? 'ekipa: ' + p.ekipaSljedeceg : 'ekipa nije dodana' }}
            </span>
            @if (s.preostalo > 0) {
              <span class="oznaka"><mat-icon inline>payments</mat-icon> još {{ s.preostalo | currency: 'EUR' : 'symbol' : '1.0-0' }}</span>
            }
          </span>
        </a>
      }

      @if (p.kasniIsporuka || p.bezKapare || p.noviUpiti) {
        <div class="paznja" aria-label="Treba pažnju">
          @if (p.kasniIsporuka) {
            <button type="button" class="p-kasni" [class.odabrano]="filter().kasniIsporuka" (click)="otvoriPaznju('kasniIsporuka')">
              <strong>{{ p.kasniIsporuka }}</strong> kasni isporuka
            </button>
          }
          @if (p.bezKapare) {
            <button type="button" class="p-kapara" [class.odabrano]="filter().bezKapare" (click)="otvoriPaznju('bezKapare')">
              <strong>{{ p.bezKapare }}</strong> bez kapare
            </button>
          }
          @if (p.noviUpiti) {
            <button type="button" class="p-upiti" [class.odabrano]="samoUpiti()" (click)="otvoriPaznju('upiti')">
              <strong>{{ p.noviUpiti }}</strong> {{ p.noviUpiti === 1 ? 'upit' : 'upita' }}
            </button>
          }
        </div>
      }
    }

    <div class="filteri">
      <mat-form-field appearance="outline" class="pretraga" subscriptSizing="dynamic">
        <mat-icon matPrefix>search</mat-icon>
        <input matInput placeholder="Traži par ili dvoranu" aria-label="Traži par ili dvoranu"
               (input)="pretraga$.next($any($event.target).value)">
      </mat-form-field>
      <button mat-stroked-button type="button" class="gumb-filter" (click)="panelOtvoren.set(!panelOtvoren())"
              [attr.aria-expanded]="panelOtvoren()" aria-label="Filteri">
        <mat-icon>tune</mat-icon>@if (brojAktivnih()) { <span class="broj">{{ brojAktivnih() }}</span> }
      </button>
    </div>

    <div class="kartice" role="tablist">
      @for (k of kartice; track k.vrijednost) {
        <button type="button" role="tab" [class.aktivna]="filter().kartica === k.vrijednost"
                [attr.aria-selected]="filter().kartica === k.vrijednost" (click)="postavi({ kartica: k.vrijednost })">
          {{ k.naziv }}
        </button>
      }
    </div>

    @if (panelOtvoren()) {
      <mat-card appearance="outlined" class="panel">
        @if (filter().kartica !== 'ISPORUKA') {
          <div class="sekcija">
            <span class="naslov-sekcije">Status <small>(bez odabira prikazuju se svi)</small></span>
            <mat-chip-listbox multiple [value]="filter().statusi" (change)="postavi({ statusi: $event.value })" aria-label="Status">
              @for (s of statusi; track s) {
                <mat-chip-option [value]="s">{{ oznakeStatusa[s] }}</mat-chip-option>
              }
            </mat-chip-listbox>
          </div>
        }

        <div class="sekcija">
          <span class="naslov-sekcije">Posebno</span>
          <mat-chip-listbox multiple [value]="posebno()" (change)="postaviPosebno($event.value)" aria-label="Posebni filteri">
            <mat-chip-option value="nenaplaceno">Nije plaćeno do kraja</mat-chip-option>
            <mat-chip-option value="bezKapare">Bez kapare</mat-chip-option>
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
      <h2 class="mjesec">{{ g.mjesec | date: 'LLLL yyyy' }} <span>{{ g.vjencanja.length }}</span></h2>
      <mat-card appearance="outlined" class="lista">
        @for (v of g.vjencanja; track v.id) {
          <app-vjencanje-red [v]="v" [prognoza]="prognoze()[v.id]" />
        }
      </mat-card>
    } @empty {
      @if (!ucitava() && !greska()) {
        <div class="prazno">
          <mat-icon>{{ filter().kartica === 'ISPORUKA' && !brojAktivnih() ? 'task_alt' : 'event_busy' }}</mat-icon>
          @if (brojAktivnih() || trazi()) {
            <p>Nijedno vjenčanje ne odgovara pretrazi.</p>
            @if (brojAktivnih()) { <button mat-stroked-button type="button" (click)="ocisti()">Očisti filtere</button> }
          } @else if (filter().kartica === 'ISPORUKA') {
            <p>Sve je isporučeno. Ovdje se pojave vjenčanja koja su prošla, a slike još nisu predane.</p>
          } @else if (filter().kartica === 'NADOLAZECA') {
            <p>Nema nadolazećih vjenčanja. Upit ili novo vjenčanje dodaješ na gumb +.</p>
            <a mat-stroked-button routerLink="/vjencanja/novo">Dodaj vjenčanje</a>
          } @else {
            <p>Ovdje će biti sva tvoja vjenčanja, poredana po mjesecima.</p>
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
    .sljedece {
      display: flex; flex-direction: column; gap: 3px; padding: 18px 20px; margin-bottom: 12px; border-radius: 20px;
      text-decoration: none; background: var(--mat-sys-primary); color: var(--mat-sys-on-primary);
    }
    .nadnaslov { font-size: 12px; font-weight: 600; letter-spacing: .06em; text-transform: uppercase; opacity: .8; }
    .par { font-size: 24px; font-weight: 650; letter-spacing: -0.02em; line-height: 1.2; }
    .gdje { font-size: 14px; opacity: .9; }
    .gdje::first-letter { text-transform: uppercase; }
    .oznake { display: flex; flex-wrap: wrap; gap: 6px; margin-top: 10px; }
    .oznaka {
      display: inline-flex; align-items: center; gap: 4px; padding: 4px 10px; border-radius: 999px; font-size: 13px;
      background: color-mix(in srgb, var(--mat-sys-on-primary) 16%, transparent);
    }
    .oznaka.upozori { background: var(--wm-upozorenje-pozadina); color: var(--wm-upozorenje); }

    .paznja { display: flex; gap: 8px; margin-bottom: 16px; }
    .paznja button {
      flex: 1; min-width: 0; display: flex; flex-direction: column; align-items: flex-start; gap: 0; padding: 10px 12px;
      border-radius: 14px; border: 2px solid transparent; cursor: pointer; font: inherit; font-size: 13px; text-align: left;
    }
    .paznja strong { font-size: 22px; font-weight: 650; line-height: 1.15; font-variant-numeric: tabular-nums; }
    .p-kasni { background: var(--wm-opasno-pozadina); color: var(--wm-opasno); }
    .p-kapara { background: var(--wm-upozorenje-pozadina); color: var(--wm-upozorenje); }
    .p-upiti { background: var(--wm-info-pozadina); color: var(--wm-info); }
    .paznja .odabrano { border-color: currentColor; }

    .filteri { display: flex; gap: 8px; align-items: center; margin-bottom: 10px; }
    .pretraga { flex: 1; min-width: 0; }
    .gumb-filter { height: 56px; min-width: 56px; padding: 0 14px; flex-shrink: 0; }
    .gumb-filter mat-icon { margin: 0; }
    .broj { margin-left: 6px; font-weight: 650; }
    .kartice { display: flex; gap: 4px; padding: 4px; margin-bottom: 12px; border-radius: 12px; background: var(--mat-sys-surface-container); }
    .kartice button {
      flex: 1; padding: 8px 4px; border: 0; border-radius: 9px; cursor: pointer; font: inherit; font-size: 14px;
      background: none; color: var(--mat-sys-on-surface-variant);
    }
    .kartice .aktivna { background: var(--mat-sys-surface); color: var(--mat-sys-on-surface); font-weight: 600; box-shadow: 0 1px 3px rgba(0, 0, 0, .12); }

    .jos { display: flex; flex-direction: column; align-items: center; gap: 8px; margin: 24px 0 8px; }
    .panel { padding: 16px; margin-bottom: 12px; display: flex; flex-direction: column; gap: 16px; }
    .sekcija { display: flex; flex-direction: column; gap: 6px; }
    .naslov-sekcije { font-size: 13px; font-weight: 600; color: var(--mat-sys-on-surface-variant); }
    .naslov-sekcije small { font-weight: 400; }
    .dno-panela { display: flex; align-items: center; gap: 8px; flex-wrap: wrap; }
    .godina { width: 160px; }
    .razmak { flex: 1; }
    .aktivni { display: block; margin-bottom: 12px; }
    .mjesec { font-size: 15px; margin: 22px 4px 8px; text-transform: capitalize; }
    .mjesec span { color: var(--mat-sys-on-surface-variant); font-weight: 400; margin-left: 4px; }
    .lista { overflow: hidden; }
    .prazno { text-align: center; padding: 40px 16px; color: var(--mat-sys-on-surface-variant); }
    .prazno mat-icon { font-size: 44px; width: 44px; height: 44px; }
    .prazno p { max-width: 340px; margin: 8px auto 16px; }
    @media (min-width: 721px) {
      .sljedece { padding: 22px 26px; }
      .paznja button { flex-direction: row; align-items: baseline; gap: 8px; font-size: 14px; }
    }
  `,
})
export class PopisComponent implements OnInit {
  private api = inject(ApiService);
  prognoze = signal<Record<number, Prognoza>>({});

  pregled = signal<Pregled | null>(null);
  vjencanja = signal<VjencanjeSazetak[]>([]);
  ukupno = signal(0);
  private stranica = 0;
  filter = signal<OdabirFiltera>(ucitajFilter());
  panelOtvoren = signal(false);
  trazi = signal(false);

  readonly izIso = izIso;
  readonly par = par;
  readonly zaKoliko = zaKoliko;
  readonly vrijeme = opisVremena;
  readonly oznakeStatusa = STATUSI;
  readonly statusi = Object.keys(STATUSI) as StatusVjencanja[];
  readonly kartice: { vrijednost: Kartica; naziv: string }[] = [
    { vrijednost: 'NADOLAZECA', naziv: 'Nadolazeća' },
    { vrijednost: 'ISPORUKA', naziv: 'Za isporuku' },
    { vrijednost: 'SVA', naziv: 'Sva' },
  ];
  readonly godine = rasponGodina();

  posebno = computed(() => {
    const f = this.filter();
    return [...(f.nenaplaceno ? ['nenaplaceno'] : []), ...(f.bezKapare ? ['bezKapare'] : []),
      ...(f.kasniIsporuka ? ['kasniIsporuka'] : [])];
  });
  samoUpiti = computed(() => {
    const f = this.filter();
    return f.kartica === 'NADOLAZECA' && f.statusi.length === 1 && f.statusi[0] === 'UPIT';
  });

  // Ono što sužava odabranu karticu, za oznake ispod pretrage
  aktivni = computed(() => {
    const f = this.filter();
    const lista: { kljuc: string; naziv: string }[] = [];
    if (f.kartica !== 'ISPORUKA') {
      for (const s of f.statusi) {
        lista.push({ kljuc: 'status:' + s, naziv: STATUSI[s] });
      }
    }
    if (f.godina) {
      lista.push({ kljuc: 'godina', naziv: String(f.godina) });
    }
    if (f.nenaplaceno) {
      lista.push({ kljuc: 'nenaplaceno', naziv: 'Nije plaćeno do kraja' });
    }
    if (f.bezKapare) {
      lista.push({ kljuc: 'bezKapare', naziv: 'Bez kapare' });
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
        this.trazi.set(!!this.tekstPretrage);
        this.ucitava.set(true);
        // Greška ne smije ugasiti pretragu, pa je hvatamo unutar switchMap
        return this.api.popis(zaBackend(this.filter()), this.tekstPretrage, 0, PO_STRANICI).pipe(
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

  postavi(promjena: Partial<OdabirFiltera>): void {
    this.filter.update((f) => ({ ...f, ...promjena }));
    spremiFilter(this.filter());
    this.ucitaj();
  }

  postaviPosebno(odabrano: string[]): void {
    this.postavi({
      nenaplaceno: odabrano.includes('nenaplaceno'),
      bezKapare: odabrano.includes('bezKapare'),
      kasniIsporuka: odabrano.includes('kasniIsporuka'),
    });
  }

  // Dodir na "Treba pažnju" otvori točno ta vjenčanja; drugi dodir vrati obični popis
  otvoriPaznju(sto: 'kasniIsporuka' | 'bezKapare' | 'upiti'): void {
    const f = this.filter();
    const vecOdabrano = sto === 'upiti' ? this.samoUpiti() : f[sto];
    if (vecOdabrano) {
      this.ocisti();
    } else if (sto === 'kasniIsporuka') {
      this.postavi({ ...ZADANI_FILTER, kartica: 'SVA', kasniIsporuka: true });
    } else if (sto === 'bezKapare') {
      this.postavi({ ...ZADANI_FILTER, bezKapare: true });
    } else {
      this.postavi({ ...ZADANI_FILTER, statusi: ['UPIT'] });
    }
  }

  ukloni(kljuc: string): void {
    if (kljuc.startsWith('status:')) {
      this.postavi({ statusi: this.filter().statusi.filter((s) => 'status:' + s !== kljuc) });
    } else if (kljuc === 'godina') {
      this.postavi({ godina: null });
    } else if (kljuc === 'nenaplaceno' || kljuc === 'bezKapare' || kljuc === 'kasniIsporuka') {
      this.postavi({ [kljuc]: false });
    }
  }

  // Kartica ostaje ista; čisti se samo ono što je sužava
  ocisti(): void {
    this.postavi({ ...ZADANI_FILTER, kartica: this.filter().kartica });
  }

  private ucitaj(): void {
    this.ucitava.set(true);
    this.greska.set('');
    this.api.popis(zaBackend(this.filter()), this.tekstPretrage, 0, PO_STRANICI).subscribe({
      next: (s) => this.prikazi(s, false),
      error: (err) => this.prikaziGresku(err),
    });
  }

  ucitajJos(): void {
    this.ucitava.set(true);
    this.api.popis(zaBackend(this.filter()), this.tekstPretrage, this.stranica + 1, PO_STRANICI).subscribe({
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
