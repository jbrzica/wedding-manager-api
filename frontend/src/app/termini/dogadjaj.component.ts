import { Component, inject, OnInit, signal } from '@angular/core';
import { CurrencyPipe, DatePipe } from '@angular/common';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatMenuModule } from '@angular/material/menu';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MatDialog } from '@angular/material/dialog';
import { DogadjajDetalji, javniLink, kratkiLinkUrl, RezervacijaTermina, TerminiApi } from './termini-api';
import { izIso } from '../core/datumi';
import { porukaGreske } from '../core/greska';

// Jedno fotografiranje: koliko je zauzeto, link za klijente i raspored po danima
@Component({
  selector: 'app-dogadjaj',
  imports: [CurrencyPipe, DatePipe, RouterLink, MatButtonModule, MatIconModule, MatMenuModule, MatProgressBarModule],
  template: `
    @if (!d()) {
      @if (greska()) { <p class="greska">{{ greska() }}</p> } @else { <mat-progress-bar mode="indeterminate" /> }
    } @else {
      @let d = this.d()!;
      <div class="vrh">
        <a mat-icon-button routerLink="/termini" aria-label="Natrag"><mat-icon>arrow_back</mat-icon></a>
        <span class="razmak"></span>
        <button mat-icon-button [matMenuTriggerFor]="vise" aria-label="Više"><mat-icon>more_horiz</mat-icon></button>
        <mat-menu #vise="matMenu">
          <a mat-menu-item [routerLink]="['/termini', d.id, 'uredi']"><mat-icon>edit</mat-icon> Uredi dane, sate i pakete</a>
          <button mat-menu-item (click)="kopiraj()"><mat-icon>content_copy</mat-icon> Kopiraj link</button>
          @if (d.kratkiLink) {
            <button mat-menu-item (click)="kopiraj(kratkiLinkUrl(d.kratkiLink))"><mat-icon>link</mat-icon> Kopiraj kratki link</button>
          }
          <a mat-menu-item [href]="link()" target="_blank" rel="noopener"><mat-icon>open_in_new</mat-icon> Otvori stranicu za klijente</a>
          <button mat-menu-item (click)="noviLink()"><mat-icon>refresh</mat-icon> Napravi novi link</button>
          <button mat-menu-item (click)="obrisi()" class="opasno"><mat-icon>delete</mat-icon> Obriši</button>
        </mat-menu>
      </div>

      <div class="sazetak">
        <div class="k"><span>{{ d.naziv }}</span><span>{{ d.otvoren ? 'Rezervacije otvorene' : 'Zatvoreno' }}</span></div>
        <div class="n">{{ d.zauzeto }} od {{ d.ukupnoTermina }} termina</div>
        <div class="m">Prihod od rezerviranih: {{ d.prihod | currency: 'EUR' : 'symbol' : '1.0-0' }}</div>
        <div class="traka"><div [style.width.%]="d.ukupnoTermina ? (d.zauzeto / d.ukupnoTermina) * 100 : 0"></div></div>
        <div class="red">
          <span>@for (p of d.poPaketima; track p.naziv) { {{ p.naziv }} {{ p.broj }}{{ $last ? '' : ' · ' }} }</span>
          <span>{{ d.ukupnoTermina - d.zauzeto }} slobodno</span>
        </div>
      </div>

      <div class="akcije">
        <button type="button" (click)="posalji()"><mat-icon>share</mat-icon><span>Pošalji link</span></button>
        <button type="button" (click)="dodaj()"><mat-icon>add</mat-icon><span>Dodaj ručno</span></button>
        <a [routerLink]="['/termini', d.id, 'uredi']"><mat-icon>edit_calendar</mat-icon><span>Dani i sati</span></a>
      </div>

      @for (dan of d.raspored; track dan.datum + dan.od) {
        <div class="dan">
          <span>{{ izIso(dan.datum) | date: 'EEEE, d. MMMM' }}</span>
          <span>{{ zauzetoNaDan(dan.mjesta) }} od {{ dan.mjesta.length }}</span>
        </div>
        <div class="lista">
          @for (m of dan.mjesta; track m.vrijeme) {
            @if (m.rezervacija; as r) {
              <div class="mjesto">
                <span class="v">{{ m.vrijeme.slice(0, 5) }}</span>
                <button type="button" class="tko" [matMenuTriggerFor]="rez" [matMenuTriggerData]="{ r }">
                  <strong>{{ r.ime }}</strong>
                  <span>{{ r.napomena || r.telefon || (r.rucno ? 'upisano ručno' : '') }}</span>
                </button>
                <span class="paket">{{ r.nazivPaketa }}</span>
                @if (r.telefon) {
                  <a mat-icon-button class="poziv" [href]="'tel:' + r.telefon" [attr.aria-label]="'Nazovi ' + r.ime"><mat-icon>call</mat-icon></a>
                }
              </div>
            } @else {
              <button type="button" class="mjesto slobodno" (click)="dodaj(dan.datum, m.vrijeme)">
                <span class="v">{{ m.vrijeme.slice(0, 5) }}</span><span>slobodno</span>
              </button>
            }
          }
        </div>
      }

      <mat-menu #rez="matMenu">
        <ng-template matMenuContent let-r="r">
          @if (r.email) { <a mat-menu-item [href]="'mailto:' + r.email"><mat-icon>mail</mat-icon> {{ r.email }}</a> }
          <button mat-menu-item (click)="otkazi(r)" class="opasno"><mat-icon>event_busy</mat-icon> Otkaži termin</button>
        </ng-template>
      </mat-menu>
    }
  `,
  styles: `
    .vrh { display: flex; align-items: center; margin: -8px -8px 4px; }
    .razmak { flex: 1; }
    .sazetak { color: #fff; background: linear-gradient(140deg, #4f5b4e, #6b7a69); border-radius: 20px; padding: 16px 18px; }
    .k { display: flex; justify-content: space-between; gap: 8px; font-size: 12px; letter-spacing: .06em; text-transform: uppercase; opacity: .9; }
    .n { font-size: 24px; font-weight: 700; margin: 6px 0 2px; }
    .m { font-size: 14px; opacity: .92; }
    .traka { height: 6px; border-radius: 6px; background: rgba(255, 255, 255, .25); margin: 12px 0 8px; overflow: hidden; }
    .traka div { height: 100%; background: #fff; border-radius: 6px; }
    .red { display: flex; justify-content: space-between; gap: 8px; font-size: 13px; opacity: .95; }
    .akcije { display: grid; grid-template-columns: repeat(3, 1fr); gap: 8px; margin: 12px 0 4px; max-width: 520px; }
    .akcije button, .akcije a { display: flex; flex-direction: column; align-items: center; gap: 4px; padding: 10px 4px;
      border: 1px solid var(--mat-sys-outline-variant); border-radius: 14px; background: var(--mat-sys-surface); cursor: pointer;
      font: inherit; font-size: 13px; color: var(--mat-sys-on-surface); text-decoration: none; }
    .akcije mat-icon { color: var(--mat-sys-primary); }
    .dan { display: flex; justify-content: space-between; margin: 20px 4px 8px; font-size: 13px; font-weight: 700;
           letter-spacing: .05em; text-transform: uppercase; color: var(--mat-sys-on-surface-variant); }
    .lista { border: 1px solid var(--mat-sys-outline-variant); border-radius: 16px; overflow: hidden; background: var(--mat-sys-surface); }
    .mjesto { display: flex; align-items: center; gap: 12px; padding: 8px 12px; min-height: 52px; width: 100%; box-sizing: border-box;
              border: 0; border-bottom: 1px solid var(--mat-sys-outline-variant); background: none; font: inherit; color: inherit; text-align: left; }
    .mjesto:last-child { border-bottom: 0; }
    .v { width: 46px; flex: none; font-weight: 700; font-variant-numeric: tabular-nums; }
    .tko { flex: 1; min-width: 0; display: flex; flex-direction: column; border: 0; background: none; padding: 0; font: inherit;
           color: inherit; text-align: left; cursor: pointer; }
    .tko span { font-size: 12.5px; color: var(--mat-sys-on-surface-variant); white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
    .paket { font-size: 11.5px; font-weight: 700; padding: 3px 8px; border-radius: 999px; flex: none;
             background: var(--mat-sys-secondary-container); color: var(--mat-sys-on-secondary-container); }
    .poziv { background: var(--wm-uspjeh-pozadina); color: var(--wm-uspjeh); flex: none; }
    .slobodno { cursor: pointer; color: var(--mat-sys-on-surface-variant); }
    .slobodno .v { font-weight: 500; }
    .opasno { color: var(--mat-sys-error); }
    .greska { color: var(--mat-sys-error); }
  `,
})
export class DogadjajComponent implements OnInit {
  private api = inject(TerminiApi);
  private router = inject(Router);
  private snack = inject(MatSnackBar);
  private dialog = inject(MatDialog);
  private id = Number(inject(ActivatedRoute).snapshot.paramMap.get('id'));

  readonly izIso = izIso;
  d = signal<DogadjajDetalji | null>(null);
  greska = signal('');
  link = () => (this.d() ? javniLink(this.d()!.javniKod) : '');

  ngOnInit(): void {
    this.ucitaj();
  }

  zauzetoNaDan(mjesta: { rezervacija: RezervacijaTermina | null }[]): number {
    return mjesta.filter((m) => m.rezervacija).length;
  }

  async posalji(): Promise<void> {
    const tekst = `${this.d()!.naziv}: odaberi i rezerviraj termin ovdje\n${this.link()}`;
    if (navigator.share) {
      try {
        await navigator.share({ text: tekst });
        return;
      } catch {
        // odustao ili dijeljenje nije dostupno, pa se kopira
      }
    }
    this.kopiraj();
  }

  readonly kratkiLinkUrl = kratkiLinkUrl;

  kopiraj(link = this.link()): void {
    navigator.clipboard.writeText(link).then(
      () => this.poruka('Link je kopiran. Zalijepi ga u objavu ili poruku.'),
      () => this.poruka(link),
    );
  }

  async dodaj(datum?: string, vrijeme?: string): Promise<void> {
    const { RucnaRezervacijaComponent } = await import('./rucna-rezervacija.component');
    this.dialog.open(RucnaRezervacijaComponent, {
      width: '460px', maxWidth: '100vw', autoFocus: false, panelClass: 'donji-list',
      data: { dogadjaj: this.d()!, datum, vrijeme },
    }).afterClosed().subscribe((spremljeno) => spremljeno && this.ucitaj());
  }

  otkazi(r: RezervacijaTermina): void {
    if (!confirm(`Otkazati termin ${r.vrijeme.slice(0, 5)} (${r.ime})? Vrijeme postaje slobodno za druge.`)) {
      return;
    }
    this.api.otkazi(r.id).subscribe({
      next: () => { this.poruka('Termin je otkazan.'); this.ucitaj(); },
      error: (err) => this.poruka(porukaGreske(err)),
    });
  }

  noviLink(): void {
    if (!confirm('Napraviti novi link? Stari link prestaje raditi, pa ga treba ponovno poslati.')) {
      return;
    }
    this.api.noviLink(this.id).subscribe({
      next: (d) => { this.d.set(d); this.poruka('Novi link je spreman.'); },
      error: (err) => this.poruka(porukaGreske(err)),
    });
  }

  obrisi(): void {
    if (!confirm('Obrisati ovo fotografiranje sa svim rezervacijama? Ovo se ne može vratiti.')) {
      return;
    }
    this.api.obrisi(this.id).subscribe({
      next: () => this.router.navigate(['/termini']),
      error: (err) => this.poruka(porukaGreske(err)),
    });
  }

  private ucitaj(): void {
    this.api.detalji(this.id).subscribe({
      next: (d) => this.d.set(d),
      error: (err) => this.greska.set(porukaGreske(err)),
    });
  }

  private poruka(tekst: string): void {
    this.snack.open(tekst, 'OK', { duration: 5000 });
  }
}
