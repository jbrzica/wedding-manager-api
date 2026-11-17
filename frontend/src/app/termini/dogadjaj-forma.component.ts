import { Component, inject, OnDestroy, OnInit, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { Observable, of, switchMap, map } from 'rxjs';
import { DogadjajDetalji, DogadjajRequest, PaketTermina, slikaUrl, TerminiApi } from './termini-api';
import { izIso, uIso } from '../core/datumi';
import { porukaGreske } from '../core/greska';

// Kao na plakatu za Božićno fotografiranje; sve se može promijeniti prije spremanja
const ZADANI_PAKETI: PaketTermina[] = [
  { naziv: 'Mini', opis: '7 obrađenih fotografija', trajanjeMinuta: 20, cijena: 100 },
  { naziv: 'Standard', opis: '10 obrađenih fotografija', trajanjeMinuta: 30, cijena: 120 },
  { naziv: 'Premium', opis: '12 obrađenih + sve ostale uspjele (oko 40–50)', trajanjeMinuta: 30, cijena: 150 },
];

@Component({
  selector: 'app-dogadjaj-forma',
  imports: [ReactiveFormsModule, RouterLink, MatButtonModule, MatIconModule, MatCardModule, MatFormFieldModule,
    MatInputModule, MatSelectModule, MatSlideToggleModule, MatDatepickerModule],
  template: `
    <div class="vrh">
      <a mat-icon-button [routerLink]="id ? ['/termini', id] : ['/termini']" aria-label="Natrag"><mat-icon>arrow_back</mat-icon></a>
      <h1>{{ id ? 'Uredi fotografiranje' : 'Novo fotografiranje' }}</h1>
    </div>

    <form [formGroup]="forma" (ngSubmit)="spremi()">
      <mat-card appearance="outlined">
        <mat-card-content class="polja">
          <mat-form-field appearance="outline" subscriptSizing="dynamic">
            <mat-label>Naziv</mat-label>
            <input matInput formControlName="naziv" placeholder="npr. Božićno fotografiranje">
          </mat-form-field>
          <mat-form-field appearance="outline" subscriptSizing="dynamic">
            <mat-label>Gdje</mat-label>
            <input matInput formControlName="lokacija" placeholder="npr. Studio Foto Brzica, Đakovo">
          </mat-form-field>
          <mat-form-field appearance="outline" subscriptSizing="dynamic">
            <mat-label>Opis za klijente (nije obavezno)</mat-label>
            <textarea matInput formControlName="opis" rows="3" placeholder="npr. Dođite u svečanoj odjeći. Plaćanje u studiju."></textarea>
          </mat-form-field>
          <div class="slika-polje">
            @if (pregled(); as url) {
              <div class="pregled" [style.background-image]="'url(' + url + ')'"></div>
            } @else {
              <div class="pregled prazno"><mat-icon>image</mat-icon></div>
            }
            <div class="slika-tekst">
              <strong>Naslovna slika</strong>
              <span class="sivo">Prikazuje se klijentima na vrhu stranice za rezervaciju. Najbolje široka fotografija.</span>
              <div class="slika-gumbi">
                <label mat-stroked-button class="odaberi" [class.zauzeto]="obradjuje()">
                  <input type="file" accept="image/jpeg,image/png,image/webp" (change)="odaberiSliku($event)" hidden>
                  {{ obradjuje() ? 'Pripremam…' : pregled() ? 'Promijeni' : 'Dodaj sliku' }}
                </label>
                @if (pregled()) { <button mat-button type="button" (click)="makniSliku()">Makni</button> }
              </div>
            </div>
          </div>
          <div class="red">
            <mat-form-field appearance="outline" subscriptSizing="dynamic">
              <mat-label>Termin svakih</mat-label>
              <mat-select formControlName="korakMinuta">
                @for (k of koraci; track k) { <mat-option [value]="k">{{ k }} min</mat-option> }
              </mat-select>
            </mat-form-field>
            <mat-slide-toggle formControlName="otvoren">Klijenti mogu rezervirati</mat-slide-toggle>
          </div>
          <p class="sivo">Termini počinju svakih {{ forma.value.korakMinuta }} min. Paket koji traje kraće ostavi ostatak kao predah.</p>
        </mat-card-content>
      </mat-card>

      <h2>Paketi</h2>
      <mat-card appearance="outlined">
        <mat-card-content formArrayName="paketi">
          @for (p of paketi.controls; track p; let i = $index) {
            <div class="paket" [formGroupName]="i">
              <mat-form-field appearance="outline" subscriptSizing="dynamic" class="naziv">
                <mat-label>Naziv</mat-label><input matInput formControlName="naziv">
              </mat-form-field>
              <mat-form-field appearance="outline" subscriptSizing="dynamic" class="broj">
                <mat-label>Min</mat-label><input matInput type="number" min="1" formControlName="trajanjeMinuta" inputmode="numeric">
              </mat-form-field>
              <mat-form-field appearance="outline" subscriptSizing="dynamic" class="broj">
                <mat-label>€</mat-label><input matInput type="number" min="0" step="0.01" formControlName="cijena" inputmode="decimal">
              </mat-form-field>
              <button mat-icon-button type="button" (click)="paketi.removeAt(i)" [disabled]="paketi.length === 1" aria-label="Ukloni paket">
                <mat-icon>close</mat-icon>
              </button>
              <mat-form-field appearance="outline" subscriptSizing="dynamic" class="opis">
                <mat-label>Što dobiju</mat-label><input matInput formControlName="opis" placeholder="npr. 10 obrađenih fotografija">
              </mat-form-field>
            </div>
          }
          <button mat-button type="button" (click)="dodajPaket()"><mat-icon>add</mat-icon> Dodaj paket</button>
        </mat-card-content>
      </mat-card>

      <h2>Dani i sati</h2>
      <mat-card appearance="outlined">
        <mat-card-content formArrayName="dani">
          @for (d of dani.controls; track d; let i = $index) {
            <div class="dan" [formGroupName]="i">
              <mat-form-field appearance="outline" subscriptSizing="dynamic" class="datum">
                <mat-label>Dan</mat-label>
                <input matInput [matDatepicker]="birac" formControlName="datum">
                <mat-datepicker-toggle matIconSuffix [for]="birac" />
                <mat-datepicker #birac />
              </mat-form-field>
              <mat-form-field appearance="outline" subscriptSizing="dynamic" class="sat">
                <mat-label>Od</mat-label><input matInput type="time" formControlName="od">
              </mat-form-field>
              <mat-form-field appearance="outline" subscriptSizing="dynamic" class="sat">
                <mat-label>Do</mat-label><input matInput type="time" formControlName="doVremena">
              </mat-form-field>
              <button mat-icon-button type="button" (click)="dani.removeAt(i)" [disabled]="dani.length === 1" aria-label="Ukloni dan">
                <mat-icon>close</mat-icon>
              </button>
            </div>
          }
          <button mat-button type="button" (click)="dodajDan()"><mat-icon>add</mat-icon> Dodaj dan</button>
          <p class="sivo">Pauzu za ručak napraviš tako da isti dan dodaš dvaput, npr. 10:00–13:00 i 14:00–19:00.</p>
        </mat-card-content>
      </mat-card>

      @if (greska()) { <p class="greska">{{ greska() }}</p> }
      <div class="gumbi">
        <a mat-button [routerLink]="id ? ['/termini', id] : ['/termini']">Odustani</a>
        <button mat-flat-button type="submit" [disabled]="sprema()">Spremi</button>
      </div>
    </form>
  `,
  styles: `
    :host { display: block; max-width: 720px; }
    .vrh { display: flex; align-items: center; gap: 4px; margin: -8px 0 8px -8px; }
    h1 { font-size: 22px; margin: 0; }
    h2 { font-size: 15px; margin: 20px 4px 8px; }
    .polja { display: flex; flex-direction: column; gap: 12px; padding-top: 16px; }
    .red { display: flex; gap: 16px; align-items: center; flex-wrap: wrap; }
    .paket { display: grid; grid-template-columns: 1fr 76px 90px 40px; gap: 8px; align-items: center; padding: 12px 0;
             border-bottom: 1px solid var(--mat-sys-outline-variant); }
    .paket .opis { grid-column: 1 / 4; }
    .dan { display: grid; grid-template-columns: 1fr 104px 104px 40px; gap: 8px; align-items: center; padding: 8px 0; }
    mat-card-content { padding-top: 8px; }
    .sivo { color: var(--mat-sys-on-surface-variant); font-size: 13.5px; margin: 4px 0 0; }
    .greska { color: var(--mat-sys-error); }
    .slika-polje { display: flex; gap: 14px; align-items: center; }
    .pregled { width: 120px; height: 80px; flex: none; border-radius: 12px; background: center / cover no-repeat;
               border: 1px solid var(--mat-sys-outline-variant); }
    .pregled.prazno { display: grid; place-items: center; color: var(--mat-sys-on-surface-variant); background: var(--mat-sys-surface-container); }
    .slika-tekst { display: flex; flex-direction: column; gap: 2px; min-width: 0; }
    .slika-tekst .sivo { margin: 0; }
    .slika-gumbi { display: flex; gap: 4px; margin-top: 6px; }
    .odaberi { cursor: pointer; display: inline-flex; align-items: center; border: 1px solid var(--mat-sys-outline);
               border-radius: 999px; padding: 0 16px; height: 36px; font-size: 14px; font-weight: 500; color: var(--mat-sys-primary); }
    .odaberi.zauzeto { pointer-events: none; opacity: .6; }
    .gumbi { display: flex; justify-content: flex-end; gap: 8px; margin: 16px 0; }
    @media (max-width: 480px) {
      .dan { grid-template-columns: 1fr 1fr 40px; }
      .dan .datum { grid-column: 1 / 3; }
    }
  `,
})
export class DogadjajFormaComponent implements OnInit, OnDestroy {
  private api = inject(TerminiApi);
  private router = inject(Router);
  private fb = inject(FormBuilder);
  readonly id = Number(inject(ActivatedRoute).snapshot.paramMap.get('id')) || null;
  readonly koraci = [15, 20, 30, 40, 45, 60, 90];

  sprema = signal(false);
  greska = signal('');
  pregled = signal<string | null>(null);
  obradjuje = signal(false);
  private novaSlika: Blob | null = null;
  private imaoSliku = false;
  private makniStaru = false;

  forma = this.fb.group({
    naziv: ['Božićno fotografiranje', [Validators.required, Validators.maxLength(200)]],
    lokacija: ['', Validators.maxLength(255)],
    opis: ['', Validators.maxLength(2000)],
    korakMinuta: [30, Validators.required],
    otvoren: [true],
    paketi: this.fb.array([] as ReturnType<DogadjajFormaComponent['paketGrupa']>[]),
    dani: this.fb.array([] as ReturnType<DogadjajFormaComponent['danGrupa']>[]),
  });

  get paketi() { return this.forma.controls.paketi; }
  get dani() { return this.forma.controls.dani; }

  ngOnInit(): void {
    if (this.id) {
      this.api.detalji(this.id).subscribe({ next: (d) => this.popuni(d), error: (err) => this.greska.set(porukaGreske(err)) });
    } else {
      ZADANI_PAKETI.forEach((p) => this.paketi.push(this.paketGrupa(p)));
      this.dani.push(this.danGrupa(null, '10:00', '18:00'));
    }
  }

  dodajPaket(): void {
    this.paketi.push(this.paketGrupa({ naziv: '', trajanjeMinuta: this.forma.value.korakMinuta ?? 30, cijena: 0 }));
  }

  // Novi dan: dan nakon zadnjeg, s istim satima, jer se najčešće slika više dana zaredom
  dodajDan(): void {
    const zadnji = this.dani.at(this.dani.length - 1)?.value;
    let datum: Date | null = null;
    if (zadnji?.datum) {
      datum = new Date(zadnji.datum);
      datum.setDate(datum.getDate() + 1);
    }
    this.dani.push(this.danGrupa(datum, zadnji?.od ?? '10:00', zadnji?.doVremena ?? '18:00'));
  }

  spremi(): void {
    if (this.forma.invalid) {
      this.forma.markAllAsTouched();
      this.greska.set('Popuni naziv, sve pakete i datume.');
      return;
    }
    const f = this.forma.getRawValue();
    const zahtjev: DogadjajRequest = {
      naziv: f.naziv!.trim(), lokacija: f.lokacija || null, opis: f.opis || null,
      korakMinuta: f.korakMinuta!, otvoren: !!f.otvoren,
      // Validatori su već provjerili da nijedno polje nije prazno
      paketi: f.paketi.map((p) => ({ id: p.id, naziv: p.naziv!.trim(), opis: p.opis || null, trajanjeMinuta: p.trajanjeMinuta!, cijena: p.cijena! })),
      dani: f.dani.map((d) => ({ datum: uIso(d.datum!), od: d.od!, doVremena: d.doVremena! })),
    };
    this.sprema.set(true);
    this.greska.set('');
    const spremanje = (this.id ? this.api.izmijeni(this.id, zahtjev) : this.api.stvori(zahtjev))
      .pipe(switchMap((d) => this.spremiSliku(d.id).pipe(map(() => d))));
    spremanje.subscribe({
      next: (d) => this.router.navigate(['/termini', d.id]),
      error: (err) => { this.greska.set(porukaGreske(err)); this.sprema.set(false); },
    });
  }

  ngOnDestroy(): void {
    this.oslobodiPregled();
  }

  // Fotografija s aparata ima i 10 MB; preglednik je smanji na 1600 px širine prije slanja
  async odaberiSliku(e: Event): Promise<void> {
    const ulaz = e.target as HTMLInputElement;
    const datoteka = ulaz.files?.[0];
    ulaz.value = '';
    if (!datoteka) {
      return;
    }
    this.obradjuje.set(true);
    this.greska.set('');
    try {
      const slika = await createImageBitmap(datoteka);
      const omjer = Math.min(1, 1600 / slika.width);
      const platno = document.createElement('canvas');
      platno.width = Math.round(slika.width * omjer);
      platno.height = Math.round(slika.height * omjer);
      platno.getContext('2d')!.drawImage(slika, 0, 0, platno.width, platno.height);
      slika.close();
      const blob = await new Promise<Blob | null>((ok) => platno.toBlob(ok, 'image/jpeg', 0.85));
      if (!blob) {
        throw new Error();
      }
      this.oslobodiPregled();
      this.novaSlika = blob;
      this.makniStaru = false;
      this.pregled.set(URL.createObjectURL(blob));
    } catch {
      this.greska.set('Ta slika se ne može otvoriti. Probaj JPG ili PNG.');
    } finally {
      this.obradjuje.set(false);
    }
  }

  makniSliku(): void {
    this.oslobodiPregled();
    this.novaSlika = null;
    this.makniStaru = this.imaoSliku;
    this.pregled.set(null);
  }

  private spremiSliku(id: number): Observable<unknown> {
    if (this.novaSlika) {
      return this.api.spremiSliku(id, this.novaSlika);
    }
    return this.makniStaru ? this.api.obrisiSliku(id) : of(null);
  }

  private oslobodiPregled(): void {
    const url = this.pregled();
    if (url?.startsWith('blob:')) {
      URL.revokeObjectURL(url);
    }
  }

  private popuni(d: DogadjajDetalji): void {
    this.imaoSliku = d.slikaVerzija > 0;
    this.pregled.set(slikaUrl(d.javniKod, d.slikaVerzija));
    this.forma.patchValue({ naziv: d.naziv, lokacija: d.lokacija ?? '', opis: d.opis ?? '', korakMinuta: d.korakMinuta, otvoren: d.otvoren });
    if (!this.koraci.includes(d.korakMinuta)) {
      this.koraci.push(d.korakMinuta);
    }
    d.paketi.forEach((p) => this.paketi.push(this.paketGrupa(p)));
    d.raspored.forEach((r) => this.dani.push(this.danGrupa(izIso(r.datum), r.od.slice(0, 5), r.doVremena.slice(0, 5))));
  }

  private paketGrupa(p: PaketTermina) {
    return this.fb.group({
      id: [p.id ?? null],
      naziv: [p.naziv, [Validators.required, Validators.maxLength(100)]],
      opis: [p.opis ?? '', Validators.maxLength(500)],
      trajanjeMinuta: [p.trajanjeMinuta, [Validators.required, Validators.min(1)]],
      cijena: [p.cijena, [Validators.required, Validators.min(0)]],
    });
  }

  private danGrupa(datum: Date | null, od: string, doVremena: string) {
    return this.fb.group({
      datum: [datum, Validators.required],
      od: [od, Validators.required],
      doVremena: [doVremena, Validators.required],
    });
  }
}
