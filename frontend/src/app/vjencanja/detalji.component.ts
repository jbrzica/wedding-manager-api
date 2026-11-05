import { Component, computed, inject, input, OnInit, signal } from '@angular/core';
import { CurrencyPipe, DatePipe } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
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
import { ApiService } from '../core/api.service';
import { Osoba, StatusVjencanja, VjencanjeDetalji, VrstaUplate } from '../core/modeli';
import { IZVORI_UPITA, STATUSI, ULOGE_SURADNIKA, VRSTE_UPLATE, kljucevi } from '../core/oznake';
import { danas, izIso } from '../core/datumi';
import { porukaGreske } from '../core/greska';
import { StatusOznakaComponent } from './status-oznaka.component';

@Component({
  selector: 'app-detalji',
  imports: [
    DatePipe, CurrencyPipe, ReactiveFormsModule, RouterLink, MatCardModule, MatButtonModule, MatIconModule,
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
          </div>
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
                    <strong>{{ c.ime }}</strong> <span class="sivo">· {{ oznakeUloga[c.uloga] }}</span>
                    @if (c.telefon) { <div><a [href]="'tel:' + c.telefon">{{ c.telefon }}</a></div> }
                  </div>
                  @if (c.honorar) { <span>{{ c.honorar | currency: 'EUR' }}</span> }
                </div>
              } @empty {
                <p class="sivo">Nema dodatne ekipe.</p>
              }
            </mat-card-content>
          </mat-card>

          @if (v.napomene || v.izvorUpita || v.rokIsporuke) {
            <mat-card appearance="outlined">
              <mat-card-header><mat-card-title>Ostalo</mat-card-title></mat-card-header>
              <mat-card-content>
                @if (v.rokIsporuke) { <p><span class="sivo">Rok isporuke:</span> {{ rok() | date: 'd. M. yyyy.' }}</p> }
                @if (v.izvorUpita) { <p><span class="sivo">Izvor upita:</span> {{ oznakeIzvora[v.izvorUpita] }}</p> }
                @if (v.napomene) { <p class="napomene">{{ v.napomene }}</p> }
              </mat-card-content>
            </mat-card>
          }
        </div>

        <div class="stupac">
          <mat-card appearance="outlined">
            <mat-card-header><mat-card-title>Financije</mat-card-title></mat-card-header>
            <mat-card-content>
              <div class="paket">
                {{ v.paket?.naziv || 'Bez paketa' }}
                @for (d of v.dodaci; track d.id) { <span class="sivo"> + {{ d.naziv }}</span> }
              </div>
              <div class="iznosi">
                <span>Dogovorena cijena</span><strong>{{ v.financije.dogovorenaCijena | currency: 'EUR' }}</strong>
                <span>Uplaćeno</span><strong>{{ v.financije.uplaceno | currency: 'EUR' }}</strong>
                <span>Preostalo</span>
                <strong [class.duguje]="v.financije.preostalo > 0">{{ v.financije.preostalo | currency: 'EUR' }}</strong>
                <span>Honorari ekipe</span><strong>− {{ v.financije.honorariEkipe | currency: 'EUR' }}</strong>
                <span>Zarada</span><strong>{{ v.financije.zarada | currency: 'EUR' }}</strong>
              </div>
              <p [class]="v.financije.kaparaPlacena ? 'kapara da' : 'kapara ne'">
                <mat-icon inline>{{ v.financije.kaparaPlacena ? 'check_circle' : 'schedule' }}</mat-icon>
                {{ v.financije.kaparaPlacena ? 'Kapara plaćena' : 'Kapara nije plaćena' }}
              </p>
            </mat-card-content>
          </mat-card>

          <mat-card appearance="outlined">
            <mat-card-header><mat-card-title>Uplate</mat-card-title></mat-card-header>
            <mat-card-content>
              @for (u of v.uplate; track u.id) {
                <div class="uplata">
                  <div>
                    <strong>{{ u.iznos | currency: 'EUR' }}</strong> <span class="sivo">· {{ oznakeVrsta[u.vrsta] }}</span>
                    <div class="sivo">{{ izIso(u.datum) | date: 'd. M. yyyy.' }} @if (u.opis) { · {{ u.opis }} }</div>
                  </div>
                  <button mat-icon-button (click)="obrisiUplatu(u.id)" aria-label="Obriši uplatu"><mat-icon>delete_outline</mat-icon></button>
                </div>
              } @empty {
                <p class="sivo">Još nema uplata.</p>
              }

              <form [formGroup]="uplata" (ngSubmit)="dodajUplatu()" class="nova-uplata">
                <mat-form-field appearance="outline" subscriptSizing="dynamic">
                  <mat-label>Iznos (€)</mat-label>
                  <input matInput type="number" min="0" step="0.01" formControlName="iznos">
                </mat-form-field>
                <mat-form-field appearance="outline" subscriptSizing="dynamic">
                  <mat-label>Vrsta</mat-label>
                  <mat-select formControlName="vrsta">
                    @for (vr of vrsteLista; track vr) { <mat-option [value]="vr">{{ oznakeVrsta[vr] }}</mat-option> }
                  </mat-select>
                </mat-form-field>
                <mat-form-field appearance="outline" subscriptSizing="dynamic">
                  <mat-label>Datum</mat-label>
                  <input matInput type="date" formControlName="datum">
                </mat-form-field>
                <mat-form-field appearance="outline" subscriptSizing="dynamic">
                  <mat-label>Opis</mat-label>
                  <input matInput formControlName="opis">
                </mat-form-field>
                <button mat-flat-button type="submit" [disabled]="uplata.invalid">Dodaj uplatu</button>
              </form>
            </mat-card-content>
          </mat-card>
        </div>
      </div>
    }
  `,
  styles: `
    .zaglavlje { display: flex; align-items: center; gap: 8px; margin-bottom: 16px; flex-wrap: wrap; }
    .naslov { flex: 1; min-width: 200px; }
    .naslov h1 { margin: 0; font-size: 24px; }
    .podnaslov { color: var(--mat-sys-on-surface-variant); }
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
    .paket { margin-bottom: 12px; }
    .iznosi { display: grid; grid-template-columns: 1fr auto; gap: 6px 16px; }
    .iznosi strong { text-align: right; font-variant-numeric: tabular-nums; }
    .duguje { color: var(--mat-sys-error); }
    .kapara { margin: 16px 0 0; font-weight: 500; }
    .kapara.da { color: #1e6b34; }
    .kapara.ne { color: #7a5b00; }
    .napomene { white-space: pre-line; }
    .nova-uplata { display: grid; grid-template-columns: 1fr 1fr; gap: 8px; margin-top: 16px; }
    .nova-uplata button { grid-column: span 2; }
    .opasno { color: var(--mat-sys-error); }
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

  id = input.required<string>();
  v = signal<VjencanjeDetalji | null>(null);
  greska = signal('');

  datum = computed(() => (this.v() ? izIso(this.v()!.datum) : null));
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
    datum: [danas(), Validators.required],
    opis: [''],
  });

  ngOnInit(): void {
    this.ucitaj();
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
    this.api.dodajUplatu(this.v()!.id, { iznos: u.iznos!, vrsta: u.vrsta!, datum: u.datum!, opis: u.opis || null }).subscribe({
      next: () => {
        // Nakon kapare je i sljedeća uplata najčešće rata
        this.uplata.reset({ iznos: null, vrsta: 'RATA', datum: danas(), opis: '' });
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

  obrisi(): void {
    if (!confirm('Obrisati cijelo vjenčanje? Ovo se ne može poništiti.')) {
      return;
    }
    this.api.obrisiVjencanje(this.v()!.id).subscribe({
      next: () => this.router.navigate(['/']),
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
      next: (v) => this.v.set(v),
      error: (err) => this.greska.set(porukaGreske(err)),
    });
  }

  private poruka(tekst: string): void {
    this.snack.open(tekst, 'OK', { duration: 4000 });
  }
}
