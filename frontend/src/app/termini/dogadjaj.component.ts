import { Component, computed, DestroyRef, ElementRef, inject, OnInit, signal } from '@angular/core';
import { CurrencyPipe, DatePipe } from '@angular/common';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatMenuModule } from '@angular/material/menu';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MatDialog } from '@angular/material/dialog';
import { DogadjajDetalji, javniLink, kratkiLinkUrl, RezervacijaTermina, TerminiApi } from './termini-api';
import { izIso, uIso } from '../core/datumi';
import { DanUKalendaru, KalendarDanaComponent } from './kalendar-dana.component';
import { NoveRezervacijeService } from './nove-rezervacije.service';
import { porukaGreske } from '../core/greska';

// Jedno fotografiranje: koliko je zauzeto, link za klijente i raspored po danima
@Component({
  selector: 'app-dogadjaj',
  imports: [CurrencyPipe, DatePipe, RouterLink, MatButtonModule, MatIconModule, MatMenuModule, MatProgressBarModule, KalendarDanaComponent],
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

      @if (daniKalendara().length) {
        <div class="kalendar">
          <app-kalendar-dana [dani]="daniKalendara()" [odabran]="odabraniDan()" (odaberi)="odabraniDan.set($event)" />
        </div>
      }

      @if (odabraniDan(); as datum) {
        <div class="dan">
          <span>{{ izIso(datum) | date: 'EEEE, d. MMMM' }}</span>
          <span>{{ zauzetoNaDan(mjestaDana()) }} od {{ mjestaDana().length }} zauzeto</span>
        </div>
        <div class="lista">
          @for (m of mjestaDana(); track m.vrijeme) {
            @if (m.rezervacija; as r) {
              <div class="mjesto" [class.istaknuto]="istaknuta() === r.id" [attr.data-rezervacija]="r.id">
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
              <button type="button" class="mjesto slobodno" (click)="dodaj(datum, m.vrijeme)">
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
    .kalendar { margin: 16px 0 0; max-width: 520px; }
    .istaknuto { animation: istakni 2.4s ease-out; scroll-margin-top: 90px; }
    @keyframes istakni { 0%, 40% { background: color-mix(in srgb, var(--mat-sys-primary) 18%, transparent); } 100% { background: transparent; } }
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
  private ruta = inject(ActivatedRoute);
  private el = inject(ElementRef<HTMLElement>);
  private nove = inject(NoveRezervacijeService);
  private destroy = inject(DestroyRef);
  private id = Number(this.ruta.snapshot.paramMap.get('id'));

  readonly izIso = izIso;
  d = signal<DogadjajDetalji | null>(null);
  greska = signal('');
  odabraniDan = signal<string | null>(null);
  istaknuta = signal<number | null>(null);
  link = () => (this.d() ? javniLink(this.d()!.javniKod) : '');

  // Isti dan može imati više blokova (npr. 10-13 i 14-18); u kalendaru je jedan dan
  daniKalendara = computed<DanUKalendaru[]>(() => {
    const poDanu = new Map<string, DanUKalendaru>();
    for (const r of this.d()?.raspored ?? []) {
      const dan = poDanu.get(r.datum) ?? { datum: r.datum, zauzeto: 0, ukupno: 0 };
      dan.ukupno += r.mjesta.length;
      dan.zauzeto += this.zauzetoNaDan(r.mjesta);
      poDanu.set(r.datum, dan);
    }
    return [...poDanu.values()].sort((a, b) => a.datum.localeCompare(b.datum));
  });

  mjestaDana = computed(() => (this.d()?.raspored ?? []).filter((r) => r.datum === this.odabraniDan()).flatMap((r) => r.mjesta));

  ngOnInit(): void {
    // Dolazak iz obavijesti ili maila: ?dan=2026-12-13&rezervacija=42 otvori taj dan i istakne termin
    const pretplata = this.ruta.queryParamMap.subscribe((q) => {
      this.zeljeniDan = q.get('dan');
      this.istaknuta.set(Number(q.get('rezervacija')) || null);
      if (this.d()) {
        this.postaviDan();
      }
    });
    this.destroy.onDestroy(() => pretplata.unsubscribe());
    this.ucitaj();
    // Tko otvori raspored, vidio je nove rezervacije
    this.nove.procitano();
  }

  private zeljeniDan: string | null = null;

  // Redom: dan iz linka, već odabrani, danas, prvi sljedeći dan, zadnji dan
  private postaviDan(): void {
    const dani = this.daniKalendara().map((d) => d.datum);
    const danas = uIso(new Date());
    const dan = [this.zeljeniDan, this.odabraniDan()].find((x) => x && dani.includes(x))
      ?? dani.find((x) => x >= danas) ?? dani[dani.length - 1] ?? null;
    this.zeljeniDan = null;
    this.odabraniDan.set(dan);
    const id = this.istaknuta();
    if (id) {
      setTimeout(() => this.el.nativeElement.querySelector(`[data-rezervacija="${id}"]`)?.scrollIntoView({ behavior: 'smooth', block: 'center' }), 50);
    }
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
      next: (d) => { this.d.set(d); this.postaviDan(); },
      error: (err) => this.greska.set(porukaGreske(err)),
    });
  }

  private poruka(tekst: string): void {
    this.snack.open(tekst, 'OK', { duration: 5000 });
  }
}
