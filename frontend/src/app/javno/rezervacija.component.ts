import { Component, computed, ElementRef, inject, OnInit, signal, viewChild } from '@angular/core';
import { CurrencyPipe, DatePipe } from '@angular/common';
import { ActivatedRoute } from '@angular/router';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { JavniDogadjaj, PotvrdaRezervacije, slikaUrl, TerminiApi } from '../termini/termini-api';
import { izIso, uIso } from '../core/datumi';
import { porukaGreske } from '../core/greska';

type Korak = 1 | 2 | 3;

// Jedno polje u kalendaru; null je prazno mjesto prije prvog dana u mjesecu
interface Polje {
  iso: string;
  dan: number;
  stanje: 'nema' | 'pun' | 'ima';
  slobodno: number;
}

const DANI_U_TJEDNU = ['Pon', 'Uto', 'Sri', 'Čet', 'Pet', 'Sub', 'Ned'];

/**
 * Javna stranica za rezervaciju termina (npr. Božićno fotografiranje). Klijent ne treba račun;
 * pristup daje tajni kod u linku. Vidi slobodna i zauzeta vremena, nikad tko je što rezervirao.
 */
@Component({
  selector: 'app-rezervacija',
  imports: [CurrencyPipe, DatePipe, ReactiveFormsModule, MatIconModule, MatProgressBarModule],
  template: `
    <div class="stranica">
      @if (ucitava()) {
        <mat-progress-bar mode="indeterminate" />
      } @else if (!d()) {
        <div class="poruka">
          <mat-icon>link_off</mat-icon>
          <h1 class="naslov">Link ne vrijedi</h1>
          <p>{{ greska() || 'Možda je zamijenjen novim. Javi se studiju.' }}</p>
        </div>
      } @else if (potvrda(); as p) {
        @if (slika()) { <div class="slika" [style.background-image]="'url(' + slika() + ')'"></div> }
        <div class="pb" [class.bez-slike]="!slika()">
          <div class="firma">{{ d()!.tvrtka }}</div>
          <h1 class="naslov">Termin je rezerviran</h1>
          <div class="kartica-potvrde">
            <mat-icon class="kvacica">check_circle</mat-icon>
            <div>
              <strong class="vel">{{ izIso(p.datum) | date: 'EEEE, d. MMMM' }} u {{ p.vrijeme.slice(0, 5) }}</strong>
              <div>{{ p.nazivPaketa }}, {{ p.trajanjeMinuta }} min · {{ p.cijena | currency: 'EUR' : 'symbol' : '1.0-2' }}</div>
              @if (p.lokacija) { <div class="sivo">{{ p.lokacija }}</div> }
            </div>
          </div>
          <p class="tekst">Potvrdu smo poslali na <strong>{{ forma.value.email }}</strong>. U mailu je i link za otkazivanje
            (najkasnije {{ p.otkazSati }} sata prije termina).</p>
          @if (p.telefonTvrtke) { <p class="sivo">Pitanja: {{ p.tvrtka }}, <a [href]="'tel:' + p.telefonTvrtke">{{ p.telefonTvrtke }}</a></p> }
        </div>
      } @else {
        @if (korak() === 1) {
          @if (slika()) { <div class="slika" [style.background-image]="'url(' + slika() + ')'"></div> }
          <div class="pb" [class.bez-slike]="!slika()">
            <div class="firma">{{ d()!.tvrtka }}</div>
            <h1 class="naslov">{{ d()!.naziv }}</h1>
            <div class="info">
              @if (raspon(); as r) { <span><mat-icon>event</mat-icon>{{ r }}</span> }
              @if (d()!.lokacija) { <span><mat-icon>location_on</mat-icon>{{ d()!.lokacija }}</span> }
            </div>
            @if (d()!.opis) { <p class="opis">{{ d()!.opis }}</p> }
          </div>
        } @else {
          <button type="button" class="natrag" (click)="natrag()">
            <mat-icon>arrow_back</mat-icon>
            @if (korak() === 2) { <span>{{ d()!.naziv }} · <b>{{ paket()?.naziv }}</b></span> } @else { <span>Tvoji podaci</span> }
          </button>
        }

        <div class="pb">
          @if (!d()!.otvoren || !imaSlobodnih()) {
            <div class="obavijest">
              <mat-icon>event_busy</mat-icon>
              <span>{{ !d()!.otvoren ? 'Rezervacije su zatvorene.' : 'Svi termini su popunjeni.' }}
                @if (d()!.telefonTvrtke) { Javi se na <a [href]="'tel:' + d()!.telefonTvrtke">{{ d()!.telefonTvrtke }}</a>. }</span>
            </div>
          } @else {
            <div class="kor" aria-hidden="true">
              <div [class.on]="korak() >= 1"></div><div [class.on]="korak() >= 2"></div><div [class.on]="korak() >= 3"></div>
            </div>

            @if (korak() === 1) {
              <h2 class="kl">Odaberi paket</h2>
              @for (p of d()!.paketi; track p.id) {
                <button type="button" class="pk" [class.on]="paketId() === p.id" (click)="paketId.set(p.id)">
                  <span class="rad"></span>
                  <span class="mid">
                    <span class="n">{{ p.naziv }}</span>
                    <span class="dd">{{ p.trajanjeMinuta }} min@if (p.opis) { · {{ p.opis }} }</span>
                  </span>
                  <span class="c">{{ p.cijena | currency: 'EUR' : 'symbol' : '1.0-2' }}</span>
                </button>
              }
            }

            @if (korak() === 2) {
              <div class="sek">Dan</div>
              <div class="kal">
                <div class="kal-vrh">
                  <button type="button" class="strelica" [disabled]="mjesecIndeks() === 0" (click)="pomakniMjesec(-1)"
                          aria-label="Prethodni mjesec"><mat-icon>chevron_left</mat-icon></button>
                  <b>{{ mjesecNaziv() }}</b>
                  <button type="button" class="strelica" [disabled]="mjesecIndeks() >= mjeseci().length - 1" (click)="pomakniMjesec(1)"
                          aria-label="Sljedeći mjesec"><mat-icon>chevron_right</mat-icon></button>
                </div>
                <div class="kal-mreza">
                  @for (t of daniUTjednu; track t) { <span class="kal-t">{{ t }}</span> }
                  @for (p of polja(); track $index) {
                    @if (p) {
                      <button type="button" class="kal-d {{ p.stanje }}" [class.on]="datum() === p.iso"
                              [disabled]="p.stanje !== 'ima'" (click)="odaberiDan(p.iso)">
                        <span class="br">{{ p.dan }}</span>
                        @if (p.stanje === 'ima') { <span class="sl">{{ p.slobodno }} slob.</span> }
                        @if (p.stanje === 'pun') { <span class="sl">puno</span> }
                      </button>
                    } @else {
                      <span></span>
                    }
                  }
                </div>
              </div>

              @if (danOdabran(); as dan) {
                <div class="sek" #vremena>{{ izIso(dan.datum) | date: 'EEEE, d. MMMM' }}</div>
                <div class="sl3">
                  @for (v of vremenaDana(); track v.vrijeme) {
                    <button type="button" class="s1" [class.z]="v.zauzeto" [class.on]="vrijeme() === v.vrijeme"
                            [disabled]="v.zauzeto" (click)="vrijeme.set(v.vrijeme)">{{ v.vrijeme.slice(0, 5) }}</button>
                  }
                </div>
                <div class="nap"><mat-icon>info_outline</mat-icon>
                  <span>Termin traje do {{ paket()?.trajanjeMinuta }} minuta. Precrtani su već rezervirani.</span></div>
              }
            }

            @if (korak() === 3) {
              <div class="sazetak">
                <mat-icon>event_available</mat-icon>
                <div><b>{{ izIso(datum()!) | date: 'EEE d. M.' }} u {{ vrijeme()!.slice(0, 5) }}</b><br>
                  <span class="sivo">{{ paket()?.naziv }} · {{ paket()?.trajanjeMinuta }} min · {{ paket()?.cijena | currency: 'EUR' : 'symbol' : '1.0-2' }}</span></div>
              </div>
              <form [formGroup]="forma" (ngSubmit)="rezerviraj()" id="podaci">
                <label class="pol" [class.kriv]="kriv('ime')">
                  <span class="l">Ime i prezime</span>
                  <input formControlName="ime" autocomplete="name">
                </label>
                <label class="pol" [class.kriv]="kriv('telefon')">
                  <span class="l">Telefon</span>
                  <input formControlName="telefon" type="tel" inputmode="tel" autocomplete="tel">
                </label>
                <label class="pol" [class.kriv]="kriv('email')">
                  <span class="l">Email (za potvrdu)</span>
                  <input formControlName="email" type="email" inputmode="email" autocomplete="email">
                </label>
                @if (kriv('email')) { <div class="polje-greska">Upiši ispravan email</div> }
                <label class="pol">
                  <span class="l">Napomena (nije obavezno)</span>
                  <textarea formControlName="napomena" rows="1" placeholder="npr. dolazimo nas četvero s psom"></textarea>
                </label>
                <!-- Ljudi ovo polje ne vide; botovi koji popune sve dobiju odbijenicu -->
                <input class="skriveno" tabindex="-1" autocomplete="off" formControlName="web" aria-hidden="true">
              </form>
              <div class="mail">
                <div class="h"><mat-icon>mail_outline</mat-icon> Potvrda stiže mailom odmah nakon rezervacije</div>
                <div class="b">
                  <b>Termin je rezerviran</b><br>
                  <span class="vel">{{ izIso(datum()!) | date: 'EEEE, d. MMMM' }} u {{ vrijeme()!.slice(0, 5) }}</span>
                  · {{ paket()?.naziv }}, {{ paket()?.cijena | currency: 'EUR' : 'symbol' : '1.0-2' }}<br>
                  <span class="sivo">Otkazivanje preko linka iz maila, najkasnije {{ d()!.otkazSati }} sata prije.</span>
                </div>
              </div>
              <p class="privatnost">Rezervacijom prihvaćaš da {{ d()!.tvrtka }} koristi tvoje podatke samo za ovaj termin.</p>
            }
            @if (greska()) { <p class="greska">{{ greska() }}</p> }
          }
        </div>

        @if (d()!.otvoren && imaSlobodnih()) {
          <div class="dno">
            <div class="dno-u">
              <div class="s">
                @if (korak() === 1) {
                  <span>Odabrano</span><b>{{ paket() ? paket()!.naziv + ' · ' + (paket()!.cijena | currency: 'EUR' : 'symbol' : '1.0-2') : '—' }}</b>
                } @else if (korak() === 2) {
                  <span>{{ paket()?.naziv }} · {{ paket()?.cijena | currency: 'EUR' : 'symbol' : '1.0-2' }}</span>
                  <b>@if (datum() && vrijeme()) { {{ izIso(datum()!) | date: 'EEE d. M.' }} u {{ vrijeme()!.slice(0, 5) }} } @else { Odaberi termin }</b>
                } @else {
                  <span>Ukupno</span><b>{{ paket()?.cijena | currency: 'EUR' : 'symbol' : '1.0-2' }}</b>
                }
              </div>
              @if (korak() < 3) {
                <button type="button" class="btn" [disabled]="!moguDalje()" (click)="dalje()">Dalje</button>
              } @else {
                <button type="submit" form="podaci" class="btn" [disabled]="salje()">{{ salje() ? 'Šaljem…' : 'Rezerviraj termin' }}</button>
              }
            </div>
          </div>
        }
      }
    </div>
  `,
  styles: `
    :host { display: block; min-height: 100vh; background: #fbfaf7; color: #1d1b18;
            --crvena: #a8232c; --linija: #e8e1d7; --siva: #6f675c; --zelena: #4f5b4e; }
    .stranica { max-width: 520px; margin: 0 auto; padding-bottom: 130px; }
    mat-icon { font-size: 18px; width: 18px; height: 18px; }

    .slika { height: 230px; background: center 30% / cover no-repeat; position: relative; }
    .slika::after { content: ''; position: absolute; inset: 0; background: linear-gradient(transparent 55%, #fbfaf7 98%); }
    .pb { padding: 0 18px; }
    .pb.bez-slike { padding-top: 28px; }
    .firma { font-size: 12px; letter-spacing: .12em; text-transform: uppercase; color: var(--zelena); font-weight: 700; margin-top: 10px; }
    .naslov { font-family: Georgia, 'Times New Roman', serif; font-style: italic; font-weight: 400; font-size: 30px;
              color: #2c2a26; margin: 2px 0 6px; line-height: 1.15; }
    .info { display: flex; flex-wrap: wrap; gap: 4px 12px; font-size: 13.5px; color: var(--siva); margin: 0 0 6px; }
    .info span { display: inline-flex; align-items: center; gap: 4px; }
    .info mat-icon { font-size: 16px; width: 16px; height: 16px; }
    .opis { font-size: 13.5px; line-height: 1.5; color: var(--siva); margin: 4px 0 8px; white-space: pre-line; }

    .natrag { display: flex; align-items: center; gap: 8px; width: 100%; border: 0; background: none; font: inherit;
              font-size: 14px; color: var(--siva); padding: 18px 18px 8px; cursor: pointer; text-align: left; }
    .natrag b { color: #1d1b18; font-weight: 600; }
    .kor { display: flex; gap: 6px; margin: 10px 0 14px; }
    .kor div { flex: 1; height: 4px; border-radius: 4px; background: #ddd8cf; }
    .kor div.on { background: var(--crvena); }
    .kl { font-size: 15px; font-weight: 700; margin: 0 0 10px; }

    .pk { width: 100%; display: flex; align-items: center; gap: 12px; text-align: left; font: inherit; color: inherit;
          border: 1.5px solid var(--linija); background: #fff; border-radius: 16px; padding: 13px 14px; margin-bottom: 10px; cursor: pointer; }
    .pk.on { border-color: var(--crvena); box-shadow: 0 0 0 3px rgba(168, 35, 44, .1); }
    .rad { width: 20px; height: 20px; border-radius: 50%; border: 2px solid #b9b2a6; flex: none; box-sizing: border-box; }
    .pk.on .rad { border: 6px solid var(--crvena); }
    .mid { flex: 1; display: flex; flex-direction: column; }
    .n { font-weight: 700; font-size: 16px; }
    .dd { font-size: 13px; color: var(--siva); margin-top: 2px; line-height: 1.35; }
    .c { font-weight: 700; font-size: 18px; white-space: nowrap; }

    .sek { font-size: 12px; font-weight: 700; letter-spacing: .08em; text-transform: uppercase; color: var(--siva); margin: 6px 2px 8px;
           scroll-margin-top: 16px; }
    .kal { background: #fff; border: 1px solid var(--linija); border-radius: 18px; padding: 10px 10px 12px; }
    .kal-vrh { display: flex; align-items: center; justify-content: space-between; padding: 0 2px 8px; font-size: 15px; }
    .kal-vrh b::first-letter { text-transform: uppercase; }
    .strelica { width: 36px; height: 36px; border-radius: 50%; border: 0; background: #f3f0ea; color: #1d1b18; cursor: pointer;
                display: grid; place-items: center; }
    .strelica mat-icon { font-size: 22px; width: 22px; height: 22px; }
    .strelica:disabled { opacity: .3; cursor: default; }
    .kal-mreza { display: grid; grid-template-columns: repeat(7, minmax(0, 1fr)); gap: 4px; }
    .kal-t { text-align: center; font-size: 11.5px; font-weight: 700; color: var(--siva); padding-bottom: 4px; }
    .kal-d { min-width: 0; aspect-ratio: 1 / 1.05; border-radius: 12px; border: 1.5px solid transparent; background: none; font: inherit; color: #c2bbb0;
             display: flex; flex-direction: column; align-items: center; justify-content: center; gap: 1px; padding: 0; cursor: default; }
    .kal-d .br { font-size: 15px; font-weight: 600; line-height: 1.1; }
    .kal-d .sl { font-size: 9.5px; line-height: 1; white-space: nowrap; overflow: hidden; max-width: 100%; }
    .kal-d.ima { border-color: var(--linija); background: #fff; color: #1d1b18; cursor: pointer; }
    .kal-d.ima .br { font-weight: 700; }
    .kal-d.ima .sl { color: var(--crvena); }
    .kal-d.pun { background: #f1eee9; color: #b7afa3; }
    .kal-d.pun .br { text-decoration: line-through; }
    .kal-d.on { background: var(--crvena); border-color: var(--crvena); color: #fff; }
    .kal-d.on .sl { color: #fff; }

    .sl3 { display: grid; grid-template-columns: repeat(3, 1fr); gap: 8px; }
    .s1 { border: 1.5px solid var(--linija); background: #fff; border-radius: 12px; padding: 12px 0; font: inherit; font-weight: 600;
          font-size: 15px; color: inherit; cursor: pointer; }
    .s1.z { background: #f1eee9; color: #b7afa3; text-decoration: line-through; border-color: #f1eee9; font-weight: 500; cursor: default; }
    .s1.on { background: var(--crvena); border-color: var(--crvena); color: #fff; }
    .nap { margin: 14px 0 0; background: #e9ece6; border-radius: 12px; padding: 10px 12px; font-size: 13px; color: #44503f;
           display: flex; gap: 8px; line-height: 1.4; align-items: flex-start; }

    .sazetak { margin: 0 0 12px; border-radius: 14px; background: #fff; border: 1px solid var(--linija); padding: 11px 12px;
               display: flex; gap: 10px; align-items: center; font-size: 14px; line-height: 1.35; }
    .sazetak mat-icon { color: var(--crvena); font-size: 22px; width: 22px; height: 22px; }
    .pol { display: block; border: 1px solid var(--linija); background: #fff; border-radius: 12px; padding: 8px 12px 9px; margin-bottom: 8px; cursor: text; }
    .pol:focus-within { border-color: #8d8579; box-shadow: 0 0 0 3px rgba(79, 91, 78, .1); }
    .pol.kriv { border-color: #b3392a; }
    .pol .l { display: block; font-size: 11.5px; color: var(--siva); }
    .pol input, .pol textarea { display: block; width: 100%; border: 0; outline: 0; padding: 2px 0 0; background: none;
                                font: inherit; font-size: 16px; color: #1d1b18; resize: none; }
    .pol textarea { field-sizing: content; min-height: 22px; }
    .pol ::placeholder { color: #b7afa3; }
    .polje-greska { color: #b3392a; font-size: 12.5px; margin: -4px 4px 8px; }
    .mail { margin: 8px 0 0; border: 1px solid var(--linija); border-radius: 14px; background: #fff; overflow: hidden; font-size: 13px; }
    .mail .h { background: #f3f1ec; padding: 8px 12px; color: var(--siva); display: flex; gap: 6px; align-items: center; }
    .mail .b { padding: 10px 12px; line-height: 1.55; }
    .privatnost { font-size: 12.5px; color: var(--siva); margin: 12px 4px 0; line-height: 1.45; }
    .vel { display: inline-block; }
    .vel::first-letter { text-transform: uppercase; }
    .skriveno { position: absolute; left: -10000px; width: 1px; height: 1px; opacity: 0; }

    .sivo { color: var(--siva); font-size: 13.5px; }
    .greska { color: #b3392a; margin: 12px 2px; }
    .obavijest { display: flex; gap: 10px; align-items: center; padding: 14px; border-radius: 14px; background: #f8ecd0; color: #8a6100; margin-top: 12px; }
    .dno { position: fixed; left: 0; right: 0; bottom: 0; background: #fff; border-top: 1px solid var(--linija);
           padding: 12px max(16px, env(safe-area-inset-right)) max(18px, env(safe-area-inset-bottom)) max(16px, env(safe-area-inset-left)); }
    .dno-u { max-width: 488px; margin: 0 auto; display: flex; align-items: center; gap: 12px; }
    .dno .s { flex: 1; text-align: left; align-items: flex-start; display: flex; flex-direction: column; font-size: 13px; color: var(--siva); line-height: 1.35; min-width: 0; }
    .dno .s b { color: #1d1b18; font-size: 15px; }
    .btn { background: var(--crvena); color: #fff; border: 0; border-radius: 999px; padding: 13px 22px; font: inherit;
           font-weight: 700; font-size: 15px; cursor: pointer; white-space: nowrap; }
    .btn:disabled { background: #d8d2c8; cursor: default; }

    .poruka { text-align: center; padding: 80px 24px; color: var(--siva); }
    .poruka mat-icon { font-size: 44px; width: 44px; height: 44px; }
    .kartica-potvrde { display: flex; gap: 12px; align-items: flex-start; background: #e4f2e8; color: #1d3b29;
                       border-radius: 16px; padding: 16px; margin: 14px 0 16px; line-height: 1.5; }
    .kvacica { color: #2e7d4f; flex: none; font-size: 24px; width: 24px; height: 24px; }
    .tekst { line-height: 1.5; }
  `,
})
export class RezervacijaComponent implements OnInit {
  private api = inject(TerminiApi);
  private kod = inject(ActivatedRoute).snapshot.paramMap.get('kod') ?? '';
  private vremenaEl = viewChild<ElementRef<HTMLElement>>('vremena');

  readonly izIso = izIso;
  readonly daniUTjednu = DANI_U_TJEDNU;
  d = signal<JavniDogadjaj | null>(null);
  ucitava = signal(true);
  greska = signal('');
  salje = signal(false);
  korak = signal<Korak>(1);
  paketId = signal<number | null>(null);
  datum = signal<string | null>(null);
  vrijeme = signal<string | null>(null);
  potvrda = signal<PotvrdaRezervacije | null>(null);
  mjesecIndeks = signal(0);

  slika = computed(() => (this.d() ? slikaUrl(this.kod, this.d()!.slikaVerzija) : null));
  paket = computed(() => this.d()?.paketi.find((p) => p.id === this.paketId()) ?? null);
  danOdabran = computed(() => this.d()?.dani.find((dan) => dan.datum === this.datum()) ?? null);
  imaSlobodnih = computed(() => !!this.d()?.dani.some((dan) => dan.slobodno.length));
  moguDalje = computed(() => (this.korak() === 1 ? !!this.paketId() : !!this.datum() && !!this.vrijeme()));

  // Slobodna i zauzeta vremena zajedno, po redu, da se vidi cijeli dan
  vremenaDana = computed(() => {
    const dan = this.danOdabran();
    if (!dan) {
      return [];
    }
    return [...dan.slobodno.map((v) => ({ vrijeme: v, zauzeto: false })), ...dan.zauzeto.map((v) => ({ vrijeme: v, zauzeto: true }))]
      .sort((a, b) => a.vrijeme.localeCompare(b.vrijeme));
  });

  raspon = computed(() => {
    const dani = this.d()?.dani ?? [];
    if (!dani.length) {
      return '';
    }
    const f = (iso: string) => { const x = izIso(iso); return `${x.getDate()}. ${x.getMonth() + 1}.`; };
    return dani.length === 1 ? f(dani[0].datum) : `${f(dani[0].datum)} – ${f(dani[dani.length - 1].datum)}`;
  });

  // Mjeseci u kojima ima dana za fotografiranje, kao prvi dan mjeseca
  mjeseci = computed(() => {
    const kljucevi = new Set((this.d()?.dani ?? []).map((dan) => dan.datum.slice(0, 7)));
    return [...kljucevi].sort().map((k) => izIso(`${k}-01`));
  });

  mjesecNaziv = computed(() => {
    const m = this.mjeseci()[this.mjesecIndeks()];
    return m ? new DatePipe('hr').transform(m, 'LLLL yyyy') ?? '' : '';
  });

  polja = computed<(Polje | null)[]>(() => {
    const prvi = this.mjeseci()[this.mjesecIndeks()];
    if (!prvi) {
      return [];
    }
    const poDatumu = new Map((this.d()?.dani ?? []).map((dan) => [dan.datum, dan]));
    const pomak = (prvi.getDay() + 6) % 7; // ponedjeljak = 0
    const brojDana = new Date(prvi.getFullYear(), prvi.getMonth() + 1, 0).getDate();
    const polja: (Polje | null)[] = Array(pomak).fill(null);
    for (let i = 1; i <= brojDana; i++) {
      const iso = uIso(new Date(prvi.getFullYear(), prvi.getMonth(), i));
      const dan = poDatumu.get(iso);
      const slobodno = dan?.slobodno.length ?? 0;
      polja.push({ iso, dan: i, slobodno, stanje: !dan ? 'nema' : slobodno ? 'ima' : 'pun' });
    }
    return polja;
  });

  forma = inject(FormBuilder).nonNullable.group({
    ime: ['', [Validators.required, Validators.maxLength(200)]],
    telefon: ['', [Validators.required, Validators.maxLength(50)]],
    email: ['', [Validators.required, Validators.email, Validators.maxLength(255)]],
    napomena: ['', Validators.maxLength(1000)],
    web: [''],
  });

  ngOnInit(): void {
    this.ucitaj();
  }

  kriv(polje: 'ime' | 'telefon' | 'email'): boolean {
    const c = this.forma.controls[polje];
    return c.invalid && c.touched;
  }

  pomakniMjesec(smjer: number): void {
    this.mjesecIndeks.update((i) => Math.min(Math.max(i + smjer, 0), this.mjeseci().length - 1));
  }

  odaberiDan(datum: string): void {
    this.datum.set(datum);
    this.vrijeme.set(null);
    // Na mobitelu su vremena ispod kalendara, pa ih se dovede na ekran
    setTimeout(() => this.vremenaEl()?.nativeElement.scrollIntoView({ behavior: 'smooth', block: 'start' }));
  }

  natrag(): void {
    this.greska.set('');
    this.korak.update((k) => (k === 3 ? 2 : 1));
  }

  dalje(): void {
    this.greska.set('');
    if (this.korak() === 1) {
      if (this.datum()) {
        this.prikaziMjesec(this.datum()!);
      } else {
        const prvi = this.d()?.dani.find((dan) => dan.slobodno.length)?.datum;
        if (prvi) {
          this.prikaziMjesec(prvi);
        }
      }
      this.korak.set(2);
    } else {
      this.korak.set(3);
    }
    window.scrollTo({ top: 0 });
  }

  rezerviraj(): void {
    if (this.forma.invalid) {
      this.forma.markAllAsTouched();
      this.greska.set('Upiši ime, telefon i email.');
      return;
    }
    if (this.salje()) {
      return;
    }
    const f = this.forma.getRawValue();
    this.salje.set(true);
    this.greska.set('');
    this.api.rezerviraj(this.kod, {
      paketId: this.paketId()!, datum: this.datum()!, vrijeme: this.vrijeme()!,
      ime: f.ime.trim(), telefon: f.telefon.trim(), email: f.email.trim(), napomena: f.napomena || null, web: f.web,
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

  private prikaziMjesec(iso: string): void {
    const i = this.mjeseci().findIndex((m) => uIso(m).slice(0, 7) === iso.slice(0, 7));
    this.mjesecIndeks.set(Math.max(i, 0));
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
