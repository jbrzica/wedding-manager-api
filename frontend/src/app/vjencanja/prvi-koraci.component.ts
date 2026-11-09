import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { ApiService } from '../core/api.service';

const KLJUC_SKRIVENO = 'wm-prvi-koraci-skriveno';
const KLJUC_ZASLON = 'wm-pocetni-zaslon-gotovo';

interface Korak {
  naslov: string;
  opis: string;
  link: string;
  queryParams?: Record<string, string>;
  gotovo: boolean;
}

function procitaj(kljuc: string): boolean {
  try {
    return localStorage.getItem(kljuc) === '1';
  } catch {
    return false;
  }
}

function zapisi(kljuc: string): void {
  try {
    localStorage.setItem(kljuc, '1');
  } catch {
    // bez pohrane lista se samo opet pojavi sljedeći put
  }
}

/**
 * Lista „Prvi koraci“ na početnoj stranici. Koraci se označavaju sami prema podacima,
 * a lista nestaje kad je sve gotovo ili kad je korisnik sakrije.
 */
@Component({
  selector: 'app-prvi-koraci',
  imports: [RouterLink, MatCardModule, MatButtonModule, MatIconModule, MatProgressBarModule],
  template: `
    @if (prikazi()) {
      <mat-card appearance="outlined" class="koraci">
        <div class="vrh">
          <div>
            <h2>Prvi koraci</h2>
            <p class="sivo">{{ gotovih() }} od {{ koraci().length }} gotovo. Za desetak minuta upoznaš sve važno.</p>
          </div>
          <button mat-button type="button" (click)="sakrij()">Sakrij</button>
        </div>
        <mat-progress-bar mode="determinate" [value]="(gotovih() / koraci().length) * 100" />
        <ol>
          @for (k of koraci(); track k.naslov) {
            <li [class.gotovo]="k.gotovo">
              <mat-icon class="kvacica">{{ k.gotovo ? 'check_circle' : 'radio_button_unchecked' }}</mat-icon>
              <div class="tekst">
                <a [routerLink]="k.link" [queryParams]="k.queryParams">{{ k.naslov }}</a>
                @if (!k.gotovo) { <span class="sivo">{{ k.opis }}</span> }
              </div>
            </li>
          }
        </ol>
        @if (!naZaslonu()) {
          <button mat-button type="button" class="oznaci" (click)="oznaciZaslon()">
            Aplikaciju sam već dodao na početni zaslon
          </button>
        }
      </mat-card>
    }
  `,
  styles: `
    .koraci { padding: 20px; margin-bottom: 16px; }
    .vrh { display: flex; justify-content: space-between; align-items: flex-start; gap: 12px; }
    h2 { margin: 0; font-size: 18px; }
    .vrh p { margin: 2px 0 12px; }
    ol { list-style: none; margin: 16px 0 0; padding: 0; display: flex; flex-direction: column; gap: 12px; }
    li { display: flex; gap: 12px; align-items: flex-start; }
    .kvacica { color: var(--mat-sys-outline); flex-shrink: 0; }
    .gotovo .kvacica { color: var(--wm-uspjeh); }
    .gotovo a { text-decoration: line-through; color: var(--mat-sys-on-surface-variant); }
    .tekst { display: flex; flex-direction: column; gap: 1px; }
    .tekst a { font-weight: 600; }
    .oznaci { margin-top: 8px; }
  `,
})
export class PrviKoraciComponent implements OnInit {
  private api = inject(ApiService);

  private podaci = signal<{ paketi: boolean; suradnici: boolean; vjencanje: boolean; kalendar: boolean } | null>(null);
  private skriveno = signal(procitaj(KLJUC_SKRIVENO));
  // Ako je aplikacija upravo otvorena s početnog zaslona, korak je sigurno gotov
  naZaslonu = signal(procitaj(KLJUC_ZASLON) || window.matchMedia?.('(display-mode: standalone)').matches);

  koraci = computed((): Korak[] => {
    const p = this.podaci();
    return [
      { naslov: 'Dodaj svoje pakete i dodatke', opis: 'Cijene se same upisuju kad odabereš paket na vjenčanju.',
        link: '/paketi', gotovo: !!p?.paketi },
      { naslov: 'Dodaj suradnike', opis: 'Snimatelji i fotografi koje stavljaš u ekipu; svaki dobije svoj link bez cijena.',
        link: '/paketi', queryParams: { tab: 'suradnici' }, gotovo: !!p?.suradnici },
      { naslov: 'Upiši prvo vjenčanje', opis: 'Datum, mladenci, paket, raspored i ekipa na jednom mjestu.',
        link: '/vjencanja/novo', gotovo: !!p?.vjencanje },
      { naslov: 'Stavi vjenčanja u kalendar na mobitelu', opis: 'Google ili Apple kalendar, sve se samo osvježava.',
        link: '/postavke', gotovo: !!p?.kalendar },
      { naslov: 'Dodaj aplikaciju na početni zaslon', opis: 'Otvara se kao prava aplikacija, preko cijelog ekrana.',
        link: '/pomoc', queryParams: { tema: 'pocetni-zaslon' }, gotovo: this.naZaslonu() },
    ];
  });
  gotovih = computed(() => this.koraci().filter((k) => k.gotovo).length);
  prikazi = computed(() => this.podaci() !== null && !this.skriveno() && this.gotovih() < this.koraci().length);

  ngOnInit(): void {
    if (!this.skriveno()) {
      this.api.prviKoraci().subscribe({ next: (p) => this.podaci.set(p), error: () => this.podaci.set(null) });
    }
  }

  sakrij(): void {
    zapisi(KLJUC_SKRIVENO);
    this.skriveno.set(true);
  }

  oznaciZaslon(): void {
    zapisi(KLJUC_ZASLON);
    this.naZaslonu.set(true);
  }
}
