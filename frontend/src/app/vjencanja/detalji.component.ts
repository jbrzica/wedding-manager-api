import { Component, computed, inject, input, OnInit, signal } from '@angular/core';
import { CurrencyPipe, DatePipe, DecimalPipe } from '@angular/common';
import { FormBuilder, FormsModule, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatMenuModule } from '@angular/material/menu';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatSnackBar } from '@angular/material/snack-bar';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { ApiService } from '../core/api.service';
import { PovijestVjencanjaComponent } from '../povijest/povijest-vjencanja.component';
import { Osoba, Prognoza, StatusVjencanja, VjencanjeDetalji, VrstaUplate, Zadatak } from '../core/modeli';
import { opisVremena } from '../core/vrijeme';
import { IZVORI_UPITA, STATUSI, ULOGE_SURADNIKA, VRSTE_UPLATE, kljucevi } from '../core/oznake';
import { izIso, uIso } from '../core/datumi';
import { porukaGreske } from '../core/greska';
import { StatusOznakaComponent } from './status-oznaka.component';

@Component({
  selector: 'app-detalji',
  imports: [PovijestVjencanjaComponent, DecimalPipe,
    DatePipe, CurrencyPipe, FormsModule, ReactiveFormsModule, MatCheckboxModule, MatDatepickerModule, RouterLink, MatCardModule, MatButtonModule, MatIconModule,
    MatMenuModule, MatFormFieldModule, MatInputModule, MatSelectModule, MatProgressBarModule, StatusOznakaComponent,
  ],
  template: `
    @if (greska()) {
      <p class="greska">{{ greska() }}</p>
    }
    @if (!v() && !greska()) {
      <mat-progress-bar mode="indeterminate" />
    }

    @if (v(); as v) {
      <div class="zaglavlje">
        <a mat-icon-button routerLink="/" aria-label="Natrag"><mat-icon>arrow_back</mat-icon></a>
        <div class="naslov">
          <h1>{{ v.mlada.ime }} &amp; {{ v.mladozenja.ime }}</h1>
          <div class="podnaslov">
            {{ datum() | date: 'EEEE, d. MMMM yyyy.' }}
            @if (v.lokacija) { · {{ v.lokacija }} }
            @if (v.brojGostiju) { · {{ v.brojGostiju }} gostiju }
          </div>
          @if (prognoza(); as p) {
            <div class="prognoza" title="Prognoza za {{ p.mjesto }}">
              {{ vrijeme(p.kod).ikona }} {{ vrijeme(p.kod).opis }} · {{ p.max | number: '1.0-0' }}° / {{ p.min | number: '1.0-0' }}°
              @if (p.oborine !== null) { · {{ p.oborine }} % kiše }
              <span class="sivo">· {{ p.mjesto }}</span>
            </div>
          }
        </div>
        <button mat-stroked-button [matMenuTriggerFor]="statusi">
          <app-status [status]="v.status" /> <mat-icon iconPositionEnd>arrow_drop_down</mat-icon>
        </button>
        <mat-menu #statusi="matMenu">
          @for (s of statusiLista; track s) {
            <button mat-menu-item (click)="promijeniStatus(s)" [disabled]="s === v.status">{{ oznakeStatusa[s] }}</button>
          }
        </mat-menu>
        <button mat-icon-button [matMenuTriggerFor]="akcije" aria-label="Više">
          <mat-icon>more_vert</mat-icon>
        </button>
        <mat-menu #akcije="matMenu">
          <a mat-menu-item [routerLink]="['/vjencanja', v.id, 'uredi']"><mat-icon>edit</mat-icon> Uredi</a>
          <button mat-menu-item (click)="podijeliRaspored()"><mat-icon>share</mat-icon> Podijeli raspored</button>
          <button mat-menu-item (click)="obrisi()" class="opasno"><mat-icon>delete</mat-icon> Obriši</button>
        </mat-menu>
      </div>

      <div class="mreza">
        <div class="stupac">
          <mat-card appearance="outlined">
            <mat-card-header><mat-card-title>Mladenci</mat-card-title></mat-card-header>
            <mat-card-content class="mladenci">
              @for (o of [v.mlada, v.mladozenja]; track $index) {
                <div class="osoba">
                  <div class="uloga">{{ $index === 0 ? 'Mlada' : 'Mladoženja' }}</div>
                  <strong>{{ punoIme(o) }}</strong>
                  @if (o.telefon) { <a [href]="'tel:' + o.telefon"><mat-icon inline>call</mat-icon> {{ o.telefon }}</a> }
                  @if (o.email) { <a [href]="'mailto:' + o.email"><mat-icon inline>mail</mat-icon> {{ o.email }}</a> }
                  @if (o.adresa) { <span><mat-icon inline>home</mat-icon> {{ o.adresa }}</span> }
                  @if (o.oib) { <span><mat-icon inline>badge</mat-icon> OIB {{ o.oib }}</span> }
                </div>
              }
            </mat-card-content>
          </mat-card>

          <mat-card appearance="outlined">
            <mat-card-header><mat-card-title>Raspored dana</mat-card-title></mat-card-header>
            <mat-card-content>
              @for (s of v.raspored; track $index) {
                <div class="stavka">
                  <span class="vrijeme">{{ s.vrijeme ? s.vrijeme.slice(0, 5) : '--:--' }}</span>
                  <div>
                    <strong>{{ s.naziv }}</strong>
                    @if (s.adresa) { <div class="sivo"><mat-icon inline>place</mat-icon> {{ s.adresa }}</div> }
                    @if (s.napomena) { <div class="sivo">{{ s.napomena }}</div> }
                  </div>
                </div>
              } @empty {
                <p class="sivo">Raspored još nije unesen.</p>
              }
            </mat-card-content>
          </mat-card>

          <mat-card appearance="outlined">
            <mat-card-header><mat-card-title>Ekipa</mat-card-title></mat-card-header>
            <mat-card-content>
              @for (c of v.ekipa; track c.suradnikId) {
                <div class="clan">
                  <div>
                    <strong>{{ c.ime }}</strong><span class="sivo">&nbsp;· {{ oznakeUloga[c.uloga] }}</span>
                    @if (c.telefon) { <div><a [href]="'tel:' + c.telefon">{{ c.telefon }}</a></div> }
                  </div>
                  @if (c.honorar) { <span>{{ c.honorar | currency: 'EUR' }}</span> }
                </div>
              } @empty {
                <p class="sivo">Nema dodatne ekipe.</p>
              }
            </mat-card-content>
          </mat-card>

          @if (v.napomene || v.izvorUpita || v.rokIsporuke || v.linkGalerije) {
            <mat-card appearance="outlined">
              <mat-card-header><mat-card-title>Ostalo</mat-card-title></mat-card-header>
              <mat-card-content>
                @if (v.rokIsporuke) { <p><span class="sivo">Rok isporuke:</span> {{ rok() | date: 'd. M. yyyy.' }}</p> }
                @if (v.izvorUpita) { <p><span class="sivo">Izvor upita:</span> {{ oznakeIzvora[v.izvorUpita] }}</p> }
                @if (v.linkGalerije) {
                  <p class="link-galerije"><span class="sivo">Galerija:</span> <a [href]="v.linkGalerije" target="_blank" rel="noopener">{{ v.linkGalerije }}</a></p>
                }
                @if (v.napomene) { <p class="napomene">{{ v.napomene }}</p> }
              </mat-card-content>
            </mat-card>
          }
        </div>

        <div class="stupac">
          <mat-card appearance="outlined">
            <mat-card-header>
              <mat-card-title>Kontrolna lista</mat-card-title>
              <mat-card-subtitle>{{ gotovihZadataka() }} / {{ v.zadaci.length }} gotovo</mat-card-subtitle>
            </mat-card-header>
            <mat-card-content>
              @for (z of v.zadaci; track z.id) {
                <div class="zadatak">
                  <mat-checkbox [checked]="z.gotovo" (change)="oznaci(z, $event.checked)">
                    <span [class.gotovo]="z.gotovo">{{ z.naziv }}</span>
                  </mat-checkbox>
                  <button mat-icon-button (click)="obrisiZadatak(z)" aria-label="Ukloni stavku"><mat-icon>close</mat-icon></button>
                </div>
              } @empty {
                <p class="sivo">Lista je prazna.</p>
              }
              <form (ngSubmit)="dodajZadatak()" class="novi-zadatak">
                <mat-form-field appearance="outline" subscriptSizing="dynamic">
                  <mat-label>Nova stavka</mat-label>
                  <input matInput name="noviZadatak" [(ngModel)]="noviZadatak" placeholder="npr. Poslati fotke za tisak">
                </mat-form-field>
                <button mat-icon-button type="submit" [disabled]="!noviZadatak.trim()" aria-label="Dodaj stavku"><mat-icon>add</mat-icon></button>
              </form>
            </mat-card-content>
          </mat-card>

          <mat-card appearance="outlined">
            <mat-card-header><mat-card-title>Financije</mat-card-title></mat-card-header>
            <mat-card-content>
              <!-- 1. Naplata: koliko je još za naplatiti -->
              <div class="naplata">
                <span class="mala-oznaka">Preostalo za naplatu</span>
                @if (v.financije.preostalo > 0) {
                  <span class="veliki-iznos">{{ v.financije.preostalo | currency: 'EUR' }}</span>
                } @else {
                  <span class="veliki-iznos placeno"><mat-icon>check_circle</mat-icon> Sve plaćeno</span>
                }
                <div class="traka" role="progressbar" [attr.aria-valuenow]="postotakPlaceno()" aria-valuemin="0" aria-valuemax="100">
                  <div class="ispuna" [style.width.%]="postotakPlaceno()"></div>
                </div>
                <div class="traka-legenda">
                  <span>Uplaćeno {{ v.financije.uplaceno | currency: 'EUR' }}</span>
                  <span>od {{ v.financije.dogovorenaCijena | currency: 'EUR' }}</span>
                </div>
                <span class="oznaka-kapare" [class.da]="v.financije.kaparaPlacena">
                  <mat-icon inline>{{ v.financije.kaparaPlacena ? 'check' : 'schedule' }}</mat-icon>
                  {{ v.financije.kaparaPlacena ? 'Kapara plaćena' : 'Kapara još nije plaćena' }}
                </span>
              </div>

              <!-- 2. Zarada: cijena minus troškovi -->
              <div class="izracun">
                <div class="mala-oznaka">Zarada</div>
                <div class="redak">
                  <span>
                    Dogovorena cijena
                    <span class="sivo">· {{ v.paket?.naziv || 'bez paketa' }}@for (d of v.dodaci; track d.id) { + {{ d.naziv }}}</span>
                  </span>
                  <span>{{ v.financije.dogovorenaCijena | currency: 'EUR' }}</span>
                </div>
                @if (v.financije.honorariEkipe > 0) {
                  <div class="redak minus"><span>Honorari ekipe</span><span>− {{ v.financije.honorariEkipe | currency: 'EUR' }}</span></div>
                }
                @if (v.financije.ostaliTroskovi > 0) {
                  <div class="redak minus"><span>Ostali troškovi</span><span>− {{ v.financije.ostaliTroskovi | currency: 'EUR' }}</span></div>
                }
                <div class="redak ukupno" [class.negativno]="v.financije.zarada < 0">
                  <span>Zarada</span><span>{{ v.financije.zarada | currency: 'EUR' }}</span>
                </div>
              </div>
            </mat-card-content>
          </mat-card>

          <mat-card appearance="outlined">
            <mat-card-header>
              <mat-card-title>Uplate</mat-card-title>
              <span class="razmak"></span>
              @if (!formaUplateOtvorena()) {
                <button mat-stroked-button (click)="otvoriFormuUplate()"><mat-icon>add</mat-icon> Dodaj uplatu</button>
              }
            </mat-card-header>
            <mat-card-content>
              @if (formaUplateOtvorena()) {
                <form [formGroup]="uplata" (ngSubmit)="dodajUplatu()" class="nova-uplata">
                  @if (v.financije.preostalo > 0) {
                    <button mat-stroked-button type="button" class="cijeli" (click)="upisiOstatak()">
                      Upiši ostatak · {{ v.financije.preostalo | currency: 'EUR' }}
                    </button>
                  }
                  <mat-form-field appearance="outline" subscriptSizing="dynamic">
                    <mat-label>Iznos (€)</mat-label>
                    <input matInput type="number" min="0" step="0.01" formControlName="iznos" inputmode="decimal">
                  </mat-form-field>
                  <mat-form-field appearance="outline" subscriptSizing="dynamic">
                    <mat-label>Vrsta</mat-label>
                    <mat-select formControlName="vrsta">
                      @for (vr of vrsteLista; track vr) { <mat-option [value]="vr">{{ oznakeVrsta[vr] }}</mat-option> }
                    </mat-select>
                  </mat-form-field>
                  <mat-form-field appearance="outline" subscriptSizing="dynamic">
                    <mat-label>Datum</mat-label>
                    <input matInput [matDatepicker]="datumUplate" formControlName="datum" (click)="datumUplate.open()">
                    <mat-datepicker-toggle matIconSuffix [for]="datumUplate" />
                    <mat-datepicker #datumUplate />
                  </mat-form-field>
                  <mat-form-field appearance="outline" subscriptSizing="dynamic">
                    <mat-label>Opis (nije obavezno)</mat-label>
                    <input matInput formControlName="opis">
                  </mat-form-field>
                  <div class="gumbi-uplate">
                    <button mat-button type="button" (click)="formaUplateOtvorena.set(false)">Odustani</button>
                    <button mat-flat-button type="submit" [disabled]="uplata.invalid">Spremi uplatu</button>
                  </div>
                </form>
              }

              @for (u of v.uplate; track u.id) {
                <div class="uplata">
                  <div class="uplata-datum">{{ izIso(u.datum) | date: 'd.M.yyyy.' }}</div>
                  <div class="uplata-opis">
                    <span class="vrsta" [class]="'vrsta ' + u.vrsta.toLowerCase()">{{ oznakeVrsta[u.vrsta] }}</span>
                    @if (u.opis) { <span class="sivo">{{ u.opis }}</span> }
                  </div>
                  <strong class="uplata-iznos">{{ u.iznos | currency: 'EUR' }}</strong>
                  <button mat-icon-button (click)="obrisiUplatu(u.id)" aria-label="Obriši uplatu"><mat-icon>delete_outline</mat-icon></button>
                </div>
              } @empty {
                @if (!formaUplateOtvorena()) { <p class="sivo">Još nema uplata.</p> }
              }
            </mat-card-content>
          </mat-card>

          <app-povijest-vjencanja [vjencanje]="v" />
        </div>
      </div>
    }
  `,
  styles: `
    .zaglavlje { display: flex; align-items: center; gap: 8px; margin-bottom: 16px; flex-wrap: wrap; }
    .naslov { flex: 1; min-width: 200px; }
    .naslov h1 { margin: 0; font-size: 24px; }
    .podnaslov { color: var(--mat-sys-on-surface-variant); }
    .prognoza { margin-top: 6px; display: inline-block; padding: 4px 10px; border-radius: 999px; font-size: 14px;
                background: var(--wm-info-pozadina); color: var(--wm-info); }
    .mreza { display: grid; grid-template-columns: 3fr 2fr; gap: 16px; align-items: start; }
    .stupac { display: flex; flex-direction: column; gap: 16px; }
    mat-card-content { padding-top: 8px; }
    .mladenci { display: grid; grid-template-columns: 1fr 1fr; gap: 16px; }
    .osoba { display: flex; flex-direction: column; gap: 4px; }
    .osoba a, .osoba span { color: inherit; font-size: 14px; text-decoration: none; }
    .uloga { font-size: 12px; text-transform: uppercase; color: var(--mat-sys-on-surface-variant); }
    .sivo { color: var(--mat-sys-on-surface-variant); font-size: 14px; }
    .stavka { display: flex; gap: 16px; padding: 8px 0; border-bottom: 1px solid var(--mat-sys-outline-variant); }
    .stavka:last-child { border-bottom: 0; }
    .vrijeme { font-weight: 500; width: 48px; flex-shrink: 0; font-variant-numeric: tabular-nums; }
    .clan, .uplata { display: flex; justify-content: space-between; align-items: center; padding: 6px 0; }
    .mala-oznaka {
      font-size: 11px; font-weight: 600; letter-spacing: .08em; text-transform: uppercase;
      color: var(--mat-sys-on-surface-variant);
    }
    .naplata { display: flex; flex-direction: column; gap: 6px; padding-bottom: 20px; border-bottom: 1px solid var(--mat-sys-outline-variant); }
    .veliki-iznos {
      font-size: 34px; font-weight: 700; letter-spacing: -0.025em; line-height: 1.15;
      font-variant-numeric: tabular-nums; color: var(--mat-sys-on-surface);
    }
    .veliki-iznos.placeno { color: var(--wm-uspjeh); display: flex; align-items: center; gap: 8px; font-size: 26px; }
    .traka { height: 8px; border-radius: 4px; background: var(--mat-sys-surface-container-high); overflow: hidden; margin-top: 6px; }
    .ispuna { height: 100%; background: var(--mat-sys-primary); border-radius: 4px; transition: width .3s; }
    .traka-legenda { display: flex; justify-content: space-between; font-size: 13px; color: var(--mat-sys-on-surface-variant); }
    .oznaka-kapare {
      align-self: flex-start; margin-top: 6px; padding: 3px 10px; border-radius: 12px; font-size: 12px; font-weight: 500;
      background: var(--wm-upozorenje-pozadina); color: var(--wm-upozorenje);
    }
    .oznaka-kapare.da { background: var(--wm-uspjeh-pozadina); color: var(--wm-uspjeh); }
    .izracun { padding-top: 16px; display: flex; flex-direction: column; gap: 8px; }
    .redak { display: flex; justify-content: space-between; gap: 12px; font-size: 15px; }
    .redak > span:last-child { font-variant-numeric: tabular-nums; white-space: nowrap; }
    .redak.minus { color: var(--mat-sys-on-surface-variant); }
    .redak.ukupno { border-top: 1px solid var(--mat-sys-outline-variant); padding-top: 10px; font-weight: 600; font-size: 17px; }
    .redak.ukupno.negativno { color: var(--mat-sys-error); }
    .razmak { flex: 1; }
    mat-card-header { align-items: center; }
    .duguje { color: var(--mat-sys-error); }
    .napomene { white-space: pre-line; }
    .nova-uplata {
      display: grid; grid-template-columns: 1fr 1fr; gap: 10px; margin-bottom: 16px; padding: 14px;
      border-radius: 10px; background: var(--mat-sys-surface-container-low);
    }
    .nova-uplata .cijeli, .gumbi-uplate { grid-column: span 2; }
    .gumbi-uplate { display: flex; justify-content: flex-end; gap: 8px; }
    .uplata {
      display: grid; grid-template-columns: 84px 1fr auto auto; align-items: center; gap: 8px;
      padding: 6px 0; border-bottom: 1px solid var(--mat-sys-outline-variant);
    }
    .uplata:last-child { border-bottom: 0; }
    .uplata-datum { font-size: 13px; color: var(--mat-sys-on-surface-variant); font-variant-numeric: tabular-nums; }
    .uplata-opis { display: flex; flex-direction: column; gap: 2px; min-width: 0; }
    .uplata-iznos { font-variant-numeric: tabular-nums; }
    .vrsta { align-self: flex-start; font-size: 12px; font-weight: 500; padding: 1px 8px; border-radius: 10px; background: var(--wm-neutralno-pozadina); color: var(--wm-neutralno); }
    .vrsta.kapara { background: var(--mat-sys-primary-container); color: var(--mat-sys-on-primary-container); }
    .opasno { color: var(--mat-sys-error); }
    .zadatak { display: flex; align-items: center; justify-content: space-between; }
    .zadatak button { opacity: .4; }
    .zadatak:hover button { opacity: 1; }
    .gotovo { text-decoration: line-through; color: var(--mat-sys-on-surface-variant); }
    .novi-zadatak { display: flex; align-items: center; gap: 4px; margin-top: 8px; }
    .novi-zadatak mat-form-field { flex: 1; }
    .link-galerije { overflow-wrap: anywhere; }
    @media (max-width: 820px) {
      .mreza { grid-template-columns: 1fr; }
    }
    @media (max-width: 480px) {
      .mladenci { grid-template-columns: 1fr; }
    }
  `,
})
export class DetaljiComponent implements OnInit {
  private api = inject(ApiService);
  private router = inject(Router);
  private snack = inject(MatSnackBar);
  prognoza = signal<Prognoza | null>(null);
  readonly vrijeme = opisVremena;

  id = input.required<string>();
  v = signal<VjencanjeDetalji | null>(null);
  greska = signal('');

  datum = computed(() => (this.v() ? izIso(this.v()!.datum) : null));
  gotovihZadataka = computed(() => this.v()?.zadaci.filter((z) => z.gotovo).length ?? 0);
  noviZadatak = '';
  rok = computed(() => (this.v()?.rokIsporuke ? izIso(this.v()!.rokIsporuke!) : null));

  readonly izIso = izIso;
  readonly oznakeStatusa = STATUSI;
  readonly oznakeVrsta = VRSTE_UPLATE;
  readonly oznakeUloga = ULOGE_SURADNIKA;
  readonly oznakeIzvora = IZVORI_UPITA;
  readonly statusiLista = kljucevi(STATUSI);
  readonly vrsteLista = kljucevi(VRSTE_UPLATE);

  uplata = inject(FormBuilder).group({
    iznos: [null as number | null, [Validators.required, Validators.min(0.01)]],
    vrsta: ['KAPARA' as VrstaUplate, Validators.required],
    datum: [new Date() as Date | null, Validators.required],
    opis: [''],
  });

  formaUplateOtvorena = signal(false);
  postotakPlaceno = computed(() => {
    const f = this.v()?.financije;
    if (!f || f.dogovorenaCijena <= 0) {
      return 0;
    }
    return Math.min(100, Math.round((f.uplaceno / f.dogovorenaCijena) * 100));
  });

  ngOnInit(): void {
    this.ucitaj();
  }

  // Prva uplata je najčešće kapara, sljedeće rate
  otvoriFormuUplate(): void {
    const v = this.v()!;
    this.uplata.reset({ iznos: null, vrsta: v.uplate.length ? 'RATA' : 'KAPARA', datum: new Date(), opis: '' });
    this.formaUplateOtvorena.set(true);
  }

  upisiOstatak(): void {
    this.uplata.patchValue({ iznos: this.v()!.financije.preostalo, vrsta: 'OSTATAK' });
  }

  punoIme(o: Osoba): string {
    return `${o.ime} ${o.prezime}`;
  }

  promijeniStatus(status: StatusVjencanja): void {
    this.api.promijeniStatus(this.v()!.id, status).subscribe({
      next: (v) => this.v.set(v),
      error: (err) => this.poruka(porukaGreske(err)),
    });
  }

  dodajUplatu(): void {
    const u = this.uplata.getRawValue();
    this.api.dodajUplatu(this.v()!.id, { iznos: u.iznos!, vrsta: u.vrsta!, datum: uIso(u.datum!), opis: u.opis || null }).subscribe({
      next: () => {
        this.formaUplateOtvorena.set(false);
        this.ucitaj();
      },
      error: (err) => this.poruka(porukaGreske(err)),
    });
  }

  obrisiUplatu(uplataId: number): void {
    if (!confirm('Obrisati ovu uplatu?')) {
      return;
    }
    this.api.obrisiUplatu(this.v()!.id, uplataId).subscribe({
      next: () => this.ucitaj(),
      error: (err) => this.poruka(porukaGreske(err)),
    });
  }

  oznaci(z: Zadatak, gotovo: boolean): void {
    this.api.oznaciZadatak(this.v()!.id, z.id, gotovo).subscribe({
      next: (novi) => this.zamijeniZadatke((lista) => lista.map((x) => (x.id === novi.id ? novi : x))),
      error: (err) => {
        this.poruka(porukaGreske(err));
        this.ucitaj();
      },
    });
  }

  dodajZadatak(): void {
    const naziv = this.noviZadatak.trim();
    this.api.dodajZadatak(this.v()!.id, naziv).subscribe({
      next: (novi) => {
        this.noviZadatak = '';
        this.zamijeniZadatke((lista) => [...lista, novi]);
      },
      error: (err) => this.poruka(porukaGreske(err)),
    });
  }

  obrisiZadatak(z: Zadatak): void {
    this.api.obrisiZadatak(this.v()!.id, z.id).subscribe({
      next: () => this.zamijeniZadatke((lista) => lista.filter((x) => x.id !== z.id)),
      error: (err) => this.poruka(porukaGreske(err)),
    });
  }

  // Mijenjamo samo listu, bez ponovnog učitavanja cijelog vjenčanja
  private zamijeniZadatke(promjena: (lista: Zadatak[]) => Zadatak[]): void {
    const v = this.v();
    if (v) {
      this.v.set({ ...v, zadaci: promjena(v.zadaci) });
    }
  }

  obrisi(): void {
    if (!confirm('Premjestiti vjenčanje u koš? Iz koša ga možete vratiti 30 dana.')) {
      return;
    }
    const id = this.v()!.id;
    this.api.obrisiVjencanje(id).subscribe({
      next: () => {
        this.router.navigate(['/']);
        this.snack.open('Vjenčanje je u košu.', 'Poništi', { duration: 8000 })
          .onAction()
          .subscribe(() => this.api.vratiIzKosa(id).subscribe(() => this.router.navigate(['/vjencanja', id])));
      },
      error: (err) => this.poruka(porukaGreske(err)),
    });
  }

  // Tekst rasporeda za poslati ekipi u WhatsApp/Viber
  async podijeliRaspored(): Promise<void> {
    const v = this.v()!;
    const redovi = [
      `${v.mlada.ime} & ${v.mladozenja.ime} — ${izIso(v.datum).toLocaleDateString('hr-HR')}`,
      ...(v.lokacija ? [v.lokacija] : []),
      '',
      ...v.raspored.map((s) =>
        `${s.vrijeme ? s.vrijeme.slice(0, 5) : '--:--'}  ${s.naziv}${s.adresa ? ' (' + s.adresa + ')' : ''}`),
    ];
    if (v.ekipa.length) {
      redovi.push('', 'Ekipa: ' + v.ekipa.map((c) => `${c.ime}${c.telefon ? ' ' + c.telefon : ''}`).join(', '));
    }
    const tekst = redovi.join('\n');

    if (navigator.share) {
      try {
        await navigator.share({ text: tekst });
        return;
      } catch {
        // korisnik je odustao ili dijeljenje nije dostupno, pa kopiramo
      }
    }
    try {
      await navigator.clipboard.writeText(tekst);
      this.poruka('Raspored je kopiran, zalijepite ga u poruku.');
    } catch {
      this.poruka('Kopiranje nije uspjelo.');
    }
  }

  private ucitaj(): void {
    this.api.detalji(Number(this.id())).subscribe({
      next: (v) => {
        this.v.set(v);
        this.ucitajPrognozu(v.id);
      },
      error: (err) => this.greska.set(porukaGreske(err)),
    });
  }

  // Samo za vjenčanja idućih 7 dana; inače servis vrati prazno i ništa se ne prikaže
  private ucitajPrognozu(id: number): void {
    this.api.prognoze().subscribe({ next: (p) => this.prognoza.set(p[id] ?? null), error: () => undefined });
  }

  private poruka(tekst: string): void {
    this.snack.open(tekst, 'OK', { duration: 4000 });
  }
}
