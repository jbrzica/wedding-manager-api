import { Component, computed, inject, OnInit, signal } from '@angular/core';
import { CurrencyPipe, DatePipe } from '@angular/common';
import { ActivatedRoute } from '@angular/router';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { JavniDogadjaj, PotvrdaRezervacije, TerminiApi } from '../termini/termini-api';
import { izIso } from '../core/datumi';
import { porukaGreske } from '../core/greska';

type Korak = 1 | 2 | 3;

/**
 * Javna stranica za rezervaciju termina (npr. Božićno fotografiranje). Klijent ne treba račun;
 * pristup daje tajni kod u linku. Vidi samo slobodna vremena, nikad tko je što rezervirao.
 */
@Component({
  selector: 'app-rezervacija',
  imports: [CurrencyPipe, DatePipe, ReactiveFormsModule, MatButtonModule, MatIconModule, MatFormFieldModule,
    MatInputModule, MatCheckboxModule, MatProgressBarModule],
  template: `
    <div class="stranica">
      @if (ucitava()) {
        <mat-progress-bar mode="indeterminate" />
      } @else if (!d()) {
        <div class="poruka">
          <mat-icon>link_off</mat-icon>
          <h1>Link ne vrijedi</h1>
          <p>{{ greska() || 'Možda je zamijenjen novim. Javi se studiju.' }}</p>
        </div>
      } @else if (potvrda(); as p) {
        <div class="zaglavlje">
          <div class="firma">{{ d()!.tvrtka }}</div>
          <h1>Termin je rezerviran</h1>
        </div>
        <div class="tijelo">
          <div class="kartica-potvrde">
            <mat-icon class="kvacica">check_circle</mat-icon>
            <div>
              <strong>{{ izIso(p.datum) | date: 'EEEE, d. MMMM' }} u {{ p.vrijeme.slice(0, 5) }}</strong>
              <div>{{ p.dogadjaj }} · {{ p.nazivPaketa }}, {{ p.trajanjeMinuta }} min · {{ p.cijena | currency: 'EUR' : 'symbol' : '1.0-2' }}</div>
              @if (p.lokacija) { <div class="sivo">{{ p.lokacija }}</div> }
            </div>
          </div>
          <p>Potvrdu smo poslali na <strong>{{ forma.value.email }}</strong>. U mailu je i link za otkazivanje, ako ne budeš mogao/la doći.</p>
          @if (p.telefonTvrtke) { <p class="sivo">Pitanja: {{ p.tvrtka }}, <a [href]="'tel:' + p.telefonTvrtke">{{ p.telefonTvrtke }}</a></p> }
        </div>
      } @else {
        <div class="zaglavlje">
          <div class="firma">{{ d()!.tvrtka }}</div>
          <h1>{{ d()!.naziv }}</h1>
          <div class="info">
            @if (raspon(); as r) { <span><mat-icon inline>event</mat-icon> {{ r }}</span> }
            @if (d()!.lokacija) { <span><mat-icon inline>location_on</mat-icon> {{ d()!.lokacija }}</span> }
          </div>
          @if (d()!.opis) { <p class="opis">{{ d()!.opis }}</p> }
        </div>

        <div class="tijelo">
          @if (!d()!.otvoren || !imaSlobodnih()) {
            <div class="obavijest">
              <mat-icon>event_busy</mat-icon>
              <span>{{ !d()!.otvoren ? 'Rezervacije su zatvorene.' : 'Svi termini su popunjeni.' }}
                @if (d()!.telefonTvrtke) { Javi se na <a [href]="'tel:' + d()!.telefonTvrtke">{{ d()!.telefonTvrtke }}</a>. }</span>
            </div>
          } @else {
            <div class="koraci" aria-hidden="true">
              <div [class.on]="korak() >= 1"></div><div [class.on]="korak() >= 2"></div><div [class.on]="korak() >= 3"></div>
            </div>

            @if (korak() === 1) {
              <h2>Odaberi paket</h2>
              @for (p of d()!.paketi; track p.id) {
                <button type="button" class="paket" [class.on]="paketId() === p.id" (click)="paketId.set(p.id)">
                  <span class="radio"></span>
                  <span class="sredina">
                    <strong>{{ p.naziv }}</strong>
                    <span class="sivo">{{ p.trajanjeMinuta }} min@if (p.opis) { · {{ p.opis }} }</span>
                  </span>
                  <span class="cijena">{{ p.cijena | currency: 'EUR' : 'symbol' : '1.0-2' }}</span>
                </button>
              }
            }

            @if (korak() === 2) {
              <button type="button" class="natrag" (click)="korak.set(1)"><mat-icon>arrow_back</mat-icon> {{ paket()?.naziv }}</button>
              <h2>Odaberi dan</h2>
              <div class="dani">
                @for (dan of d()!.dani; track dan.datum) {
                  <button type="button" class="dan" [class.on]="datum() === dan.datum" [disabled]="!dan.slobodno.length"
                          (click)="odaberiDan(dan.datum)">
                    <span class="a">{{ izIso(dan.datum) | date: 'EEE' }}</span>
                    <span class="b">{{ izIso(dan.datum) | date: 'd. M.' }}</span>
                    <span class="c">{{ dan.slobodno.length ? dan.slobodno.length + ' slob.' : 'popunjeno' }}</span>
                  </button>
                }
              </div>
              @if (danOdabran(); as dan) {
                <h2>{{ izIso(dan.datum) | date: 'EEEE, d. MMMM' }}</h2>
                <div class="vremena">
                  @for (v of dan.slobodno; track v) {
                    <button type="button" class="vrijeme" [class.on]="vrijeme() === v" (click)="vrijeme.set(v)">{{ v.slice(0, 5) }}</button>
                  }
                </div>
                <p class="sivo mali">Prikazana su samo slobodna vremena.</p>
              }
            }

            @if (korak() === 3) {
              <button type="button" class="natrag" (click)="korak.set(2)"><mat-icon>arrow_back</mat-icon> Promijeni termin</button>
              <div class="sazetak">
                <mat-icon>event_available</mat-icon>
                <div><strong>{{ izIso(datum()!) | date: 'EEE d. M.' }} u {{ vrijeme()!.slice(0, 5) }}</strong><br>
                  <span class="sivo">{{ paket()?.naziv }} · {{ paket()?.trajanjeMinuta }} min · {{ paket()?.cijena | currency: 'EUR' : 'symbol' : '1.0-2' }}</span></div>
              </div>
              <form [formGroup]="forma" (ngSubmit)="rezerviraj()" id="podaci" class="forma">
                <mat-form-field appearance="outline" subscriptSizing="dynamic">
                  <mat-label>Ime i prezime</mat-label>
                  <input matInput formControlName="ime" autocomplete="name">
                </mat-form-field>
                <mat-form-field appearance="outline" subscriptSizing="dynamic">
                  <mat-label>Telefon</mat-label>
                  <input matInput formControlName="telefon" type="tel" inputmode="tel" autocomplete="tel">
                </mat-form-field>
                <mat-form-field appearance="outline" subscriptSizing="dynamic">
                  <mat-label>Email (za potvrdu)</mat-label>
                  <input matInput formControlName="email" type="email" autocomplete="email">
                  <mat-error>Upiši ispravan email</mat-error>
                </mat-form-field>
                <mat-form-field appearance="outline" subscriptSizing="dynamic">
                  <mat-label>Napomena (nije obavezno)</mat-label>
                  <textarea matInput formControlName="napomena" rows="2" placeholder="npr. dolazimo nas četvero"></textarea>
                </mat-form-field>
                <!-- Ljudi ovo polje ne vide; botovi koji popune sve dobiju odbijenicu -->
                <input class="skriveno" tabindex="-1" autocomplete="off" formControlName="web" aria-hidden="true">
                <mat-checkbox formControlName="privola">
                  Pristajem da {{ d()!.tvrtka }} čuva moje podatke radi ovog termina.
                </mat-checkbox>
                <p class="sivo rok">Ako ne možeš doći, termin otkažeš linkom iz maila najkasnije {{ d()!.otkazSati }} sata prije.</p>
              </form>
            }
            @if (greska()) { <p class="greska">{{ greska() }}</p> }
          }
        </div>

        @if (d()!.otvoren && imaSlobodnih()) {
          <div class="dno">
            <div class="izbor">
              @if (paket(); as p) {
                <span class="sivo">{{ p.naziv }} · {{ p.cijena | currency: 'EUR' : 'symbol' : '1.0-2' }}</span>
                @if (datum() && vrijeme()) { <strong>{{ izIso(datum()!) | date: 'EEE d. M.' }} u {{ vrijeme()!.slice(0, 5) }}</strong> }
              } @else {
                <span class="sivo">Odaberi paket</span>
              }
            </div>
            @if (korak() < 3) {
              <button mat-flat-button type="button" [disabled]="!moguDalje()" (click)="dalje()">Dalje</button>
            } @else {
              <button mat-flat-button type="submit" form="podaci" [disabled]="forma.invalid || salje()">Rezerviraj termin</button>
            }
          </div>
        }
      }
    </div>
  `,
  styles: `
    :host { display: block; min-height: 100vh; background: #fbfaf7; color: #1d1b18;
            --crvena: #a8232c; --mat-sys-primary: #a8232c; --mat-sys-on-primary: #fff; }
    .stranica { max-width: 560px; margin: 0 auto; padding-bottom: 120px; }
    .zaglavlje { padding: 32px 20px 24px; color: #fff; background: linear-gradient(150deg, #3f4a3e, #66745f);
                 border-radius: 0 0 24px 24px; }
    .firma { font-size: 12px; letter-spacing: .12em; text-transform: uppercase; font-weight: 700; opacity: .85; }
    h1 { font-family: Georgia, 'Times New Roman', serif; font-style: italic; font-weight: 400; font-size: 32px; margin: 6px 0 8px; }
    .zaglavlje .info { display: flex; flex-wrap: wrap; gap: 4px 14px; font-size: 14px; opacity: .92; color: #fff; margin: 0; }
    .info span { display: inline-flex; align-items: center; gap: 4px; }
    .zaglavlje .opis { color: #fff; margin: 12px 0 0; font-size: 14.5px; line-height: 1.5; opacity: .95; white-space: pre-line; }
    .tijelo { padding: 16px 16px 0; }
    .koraci { display: flex; gap: 6px; margin: 4px 0 16px; }
    .koraci div { flex: 1; height: 4px; border-radius: 4px; background: #ddd8cf; }
    .koraci div.on { background: var(--crvena); }
    h2 { font-size: 15px; margin: 16px 2px 10px; }
    h2::first-letter { text-transform: uppercase; }
    button.paket { width: 100%; display: flex; align-items: center; gap: 12px; text-align: left; font: inherit; color: inherit;
                   border: 1.5px solid #e8e1d7; background: #fff; border-radius: 16px; padding: 14px; margin-bottom: 10px; cursor: pointer; }
    .paket.on { border-color: var(--crvena); box-shadow: 0 0 0 3px rgba(168, 35, 44, .1); }
    .radio { width: 20px; height: 20px; border-radius: 50%; border: 2px solid #b9b2a6; flex: none; }
    .paket.on .radio { border: 6px solid var(--crvena); }
    .sredina { flex: 1; display: flex; flex-direction: column; gap: 2px; }
    .cijena { font-weight: 700; font-size: 18px; white-space: nowrap; }
    .sivo { color: #6f675c; font-size: 13.5px; }
    .rok { margin: 8px 2px 0; }
    .mali { font-size: 12.5px; margin-top: 10px; }
    .natrag { display: inline-flex; align-items: center; gap: 6px; border: 0; background: none; font: inherit; color: #6f675c; padding: 4px 0; cursor: pointer; }
    .dani { display: flex; gap: 8px; overflow-x: auto; padding-bottom: 4px; }
    .dan { flex: none; width: 82px; display: flex; flex-direction: column; align-items: center; gap: 1px; padding: 8px 0;
           border: 1.5px solid #e8e1d7; background: #fff; border-radius: 14px; font: inherit; color: inherit; cursor: pointer; }
    .dan .a { font-size: 11.5px; font-weight: 700; text-transform: uppercase; color: #6f675c; }
    .dan .b { font-size: 18px; font-weight: 700; }
    .dan .c { font-size: 11px; color: #6f675c; }
    .dan.on { border-color: var(--crvena); background: #fbefef; }
    .dan:disabled { opacity: .45; cursor: default; }
    .vremena { display: grid; grid-template-columns: repeat(auto-fill, minmax(86px, 1fr)); gap: 8px; }
    .vrijeme { border: 1.5px solid #e8e1d7; background: #fff; border-radius: 12px; padding: 12px 0; font: inherit; font-weight: 600;
               font-size: 15px; color: inherit; cursor: pointer; }
    .vrijeme.on { background: var(--crvena); border-color: var(--crvena); color: #fff; }
    .sazetak { display: flex; gap: 12px; align-items: center; background: #fff; border: 1px solid #e8e1d7; border-radius: 14px;
               padding: 12px; margin: 8px 0 16px; }
    .sazetak mat-icon { color: var(--crvena); }
    .forma { display: flex; flex-direction: column; gap: 12px; }
    .skriveno { position: absolute; left: -10000px; width: 1px; height: 1px; opacity: 0; }
    .greska { color: #b3392a; margin: 12px 2px; }
    .dno { position: fixed; left: 0; right: 0; bottom: 0; background: #fff; border-top: 1px solid #e8e1d7;
           padding: 12px max(16px, env(safe-area-inset-right)) max(14px, env(safe-area-inset-bottom)) max(16px, env(safe-area-inset-left));
           display: flex; align-items: center; gap: 12px; justify-content: center; }
    .izbor { flex: 1; max-width: 400px; display: flex; flex-direction: column; font-size: 14.5px; }
    .dno button { border-radius: 999px; padding: 0 22px; height: 46px; }
    .obavijest { display: flex; gap: 10px; align-items: center; padding: 14px; border-radius: 14px; background: #f8ecd0; color: #8a6100; }
    .poruka { text-align: center; padding: 80px 24px; color: #6f675c; }
    .poruka h1 { color: #1d1b18; }
    .poruka mat-icon { font-size: 44px; width: 44px; height: 44px; }
    .kartica-potvrde { display: flex; gap: 12px; align-items: flex-start; background: #e4f2e8; color: #1d3b29;
                       border-radius: 16px; padding: 16px; margin: 4px 0 16px; line-height: 1.5; }
    .kvacica { color: #2e7d4f; flex: none; }
  `,
})
export class RezervacijaComponent implements OnInit {
  private api = inject(TerminiApi);
  private kod = inject(ActivatedRoute).snapshot.paramMap.get('kod') ?? '';

  readonly izIso = izIso;
  d = signal<JavniDogadjaj | null>(null);
  ucitava = signal(true);
  greska = signal('');
  salje = signal(false);
  korak = signal<Korak>(1);
  paketId = signal<number | null>(null);
  datum = signal<string | null>(null);
  vrijeme = signal<string | null>(null);
  potvrda = signal<PotvrdaRezervacije | null>(null);

  paket = computed(() => this.d()?.paketi.find((p) => p.id === this.paketId()) ?? null);
  danOdabran = computed(() => this.d()?.dani.find((dan) => dan.datum === this.datum()) ?? null);
  imaSlobodnih = computed(() => !!this.d()?.dani.some((dan) => dan.slobodno.length));
  raspon = computed(() => {
    const dani = this.d()?.dani ?? [];
    if (!dani.length) {
      return '';
    }
    const f = (iso: string) => { const x = izIso(iso); return `${x.getDate()}. ${x.getMonth() + 1}.`; };
    return dani.length === 1 ? f(dani[0].datum) : `${f(dani[0].datum)} – ${f(dani[dani.length - 1].datum)}`;
  });
  moguDalje = computed(() => (this.korak() === 1 ? !!this.paketId() : !!this.datum() && !!this.vrijeme()));

  forma = inject(FormBuilder).nonNullable.group({
    ime: ['', [Validators.required, Validators.maxLength(200)]],
    telefon: ['', [Validators.required, Validators.maxLength(50)]],
    email: ['', [Validators.required, Validators.email, Validators.maxLength(255)]],
    napomena: ['', Validators.maxLength(1000)],
    privola: [false, Validators.requiredTrue],
    web: [''],
  });

  ngOnInit(): void {
    this.ucitaj();
  }

  odaberiDan(datum: string): void {
    this.datum.set(datum);
    this.vrijeme.set(null);
  }

  dalje(): void {
    this.greska.set('');
    if (this.korak() === 1) {
      // Prvi dan sa slobodnim terminom je već odabran, da se manje dodiruje
      if (!this.datum()) {
        this.datum.set(this.d()?.dani.find((dan) => dan.slobodno.length)?.datum ?? null);
      }
      this.korak.set(2);
    } else {
      this.korak.set(3);
    }
    window.scrollTo({ top: 0 });
  }

  rezerviraj(): void {
    if (this.forma.invalid || this.salje()) {
      return;
    }
    const f = this.forma.getRawValue();
    this.salje.set(true);
    this.greska.set('');
    this.api.rezerviraj(this.kod, {
      paketId: this.paketId()!, datum: this.datum()!, vrijeme: this.vrijeme()!,
      ime: f.ime.trim(), telefon: f.telefon.trim(), email: f.email.trim(), napomena: f.napomena || null,
      privola: f.privola, web: f.web,
    }).subscribe({
      next: (p) => {
        this.potvrda.set(p);
        this.salje.set(false);
        window.scrollTo({ top: 0 });
      },
      error: (err: HttpErrorResponse) => {
        this.salje.set(false);
        this.greska.set(porukaGreske(err));
        // Netko je u međuvremenu uzeo termin: vrati na odabir vremena sa svježim popisom
        if (err.status === 409 || err.status === 400) {
          this.vrijeme.set(null);
          this.korak.set(2);
          this.ucitaj();
        }
      },
    });
  }

  private ucitaj(): void {
    this.api.javniDogadjaj(this.kod).subscribe({
      next: (d) => {
        this.d.set(d);
        this.ucitava.set(false);
      },
      error: (err) => {
        this.greska.set(err.status === 404 ? '' : porukaGreske(err));
        this.ucitava.set(false);
      },
    });
  }
}
