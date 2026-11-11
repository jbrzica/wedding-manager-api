import { Component, inject, OnInit, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatExpansionModule } from '@angular/material/expansion';
import { MatSnackBar } from '@angular/material/snack-bar';
import { ApiService } from '../core/api.service';
import { AuthService } from '../core/auth.service';
import { PredlozakUgovora } from '../core/modeli';
import { imeDatoteke, spremiIliPodijeli } from '../core/datoteka';
import { porukaGreske, porukaGreskeDatoteke } from '../core/greska';

// Oznake koje aplikacija zna popuniti; isti popis je u UgovorService na backendu
const OZNAKE: { oznaka: string; opis: string }[] = [
  { oznaka: 'narucitelj', opis: 'Ana Horvat i Ivo Kovač' },
  { oznaka: 'datum', opis: 'datum vjenčanja, npr. 7. 5. 2027.' },
  { oznaka: 'mlada', opis: 'ime i prezime mlade' },
  { oznaka: 'mlada_adresa', opis: 'adresa mlade' },
  { oznaka: 'mlada_telefon', opis: 'telefon mlade' },
  { oznaka: 'mlada_email', opis: 'email mlade' },
  { oznaka: 'mlada_oib', opis: 'OIB mlade' },
  { oznaka: 'mladozenja', opis: 'ime i prezime mladoženje (i _adresa, _telefon, _email, _oib)' },
  { oznaka: 'vjencanje', opis: 'crkva ili matičar iz rasporeda dana' },
  { oznaka: 'dvorana', opis: 'lokacija (sala)' },
  { oznaka: 'paket', opis: 'naziv paketa' },
  { oznaka: 'cijena', opis: 'dogovorena cijena, npr. 2.800 €' },
  { oznaka: 'akontacija', opis: 'zbroj uplaćenih kapara' },
  { oznaka: 'uplaceno', opis: 'sve dosad uplaćeno' },
  { oznaka: 'ostatak', opis: 'koliko je još za platiti' },
  { oznaka: 'dodaci', opis: 'dodaci odvojeni zarezom' },
  { oznaka: 'broj_gostiju', opis: 'broj gostiju' },
  { oznaka: 'napomene', opis: 'napomene s vjenčanja' },
  { oznaka: 'danas', opis: 'današnji datum' },
  { oznaka: 'tvrtka', opis: 'naziv tvrtke (i _adresa, _oib, _telefon, _email)' },
];

@Component({
  selector: 'app-ugovor-predlozak',
  imports: [DatePipe, MatCardModule, MatButtonModule, MatIconModule, MatExpansionModule],
  template: `
    <mat-card appearance="outlined">
      <mat-card-header>
        <mat-card-title>Ugovor</mat-card-title>
        <mat-card-subtitle>Word predložak iz kojeg se radi ugovor za svako vjenčanje</mat-card-subtitle>
      </mat-card-header>
      <mat-card-content>
        @if (info(); as i) {
          <p>
            @if (i.vlastiti) {
              Koristi se vaš predložak <strong>{{ i.nazivDatoteke }}</strong>
              <span class="sivo"> · učitan {{ i.azurirano | date: 'd. M. yyyy.' }}</span>
            } @else {
              Koristi se zadani predložak aplikacije.
            }
          </p>
        }
        <div class="gumbi">
          <button mat-stroked-button type="button" (click)="preuzmi()"><mat-icon>download</mat-icon> Preuzmi predložak</button>
          @if (auth.vlasnik()) {
            <button mat-stroked-button type="button" (click)="birac.click()"><mat-icon>upload_file</mat-icon> Učitaj svoj (.docx)</button>
            <input #birac type="file" hidden
                   accept=".docx,application/vnd.openxmlformats-officedocument.wordprocessingml.document"
                   (change)="ucitaj(birac)">
            @if (info()?.vlastiti) {
              <button mat-button type="button" (click)="vratiZadani()">Vrati zadani</button>
            }
          }
        </div>
        <p class="sivo">
          Preuzmite predložak, u Wordu promijenite tekst ili dodajte logo i učitajte ga natrag.
          Na mjesta gdje treba doći podatak upišite oznaku u vitičastim zagradama, npr. {{ primjer }}.
          Ako podatak nije upisan, u ugovoru je crta za dopisivanje rukom.
        </p>
        <mat-expansion-panel class="oznake">
          <mat-expansion-panel-header><mat-panel-title>Popis oznaka</mat-panel-title></mat-expansion-panel-header>
          @for (o of oznake; track o.oznaka) {
            <div class="oznaka"><code>{{ uOznaku(o.oznaka) }}</code><span class="sivo">{{ o.opis }}</span></div>
          }
        </mat-expansion-panel>
      </mat-card-content>
    </mat-card>
  `,
  styles: `
    mat-card-content > p { margin: 8px 0; }
    .gumbi { display: flex; gap: 8px; flex-wrap: wrap; justify-content: flex-start; margin: 8px 0; }
    .oznake { margin-top: 8px; box-shadow: none; border: 1px solid var(--mat-sys-outline-variant); }
    .oznaka { display: flex; flex-wrap: wrap; gap: 4px 12px; padding: 6px 0; font-size: 14px;
              border-bottom: 1px solid var(--mat-sys-outline-variant); }
    .oznaka:last-child { border-bottom: none; }
    code { font-size: 13px; min-width: 190px; }
  `,
})
export class UgovorPredlozakComponent implements OnInit {
  private api = inject(ApiService);
  private snack = inject(MatSnackBar);
  auth = inject(AuthService);

  info = signal<PredlozakUgovora | null>(null);
  readonly oznake = OZNAKE;
  // Vitičaste zagrade se ne mogu napisati izravno u predlošku, Angular ih čita kao svoje
  readonly primjer = this.uOznaku('narucitelj');

  uOznaku(ime: string): string {
    return '{' + '{' + ime + '}' + '}';
  }

  ngOnInit(): void {
    this.api.predlozakUgovora().subscribe((i) => this.info.set(i));
  }

  preuzmi(): void {
    this.api.preuzmiPredlozakUgovora().subscribe({
      next: async (odgovor) => {
        if (!(await spremiIliPodijeli(odgovor.body!, imeDatoteke(odgovor, 'predlozak-ugovora.docx')))) {
          this.snack.open('Predložak je spreman.', 'Otvori', { duration: 8000 }).onAction()
            .subscribe(() => spremiIliPodijeli(odgovor.body!, imeDatoteke(odgovor, 'predlozak-ugovora.docx')));
        }
      },
      error: async (err) => this.poruka(await porukaGreskeDatoteke(err)),
    });
  }

  ucitaj(birac: HTMLInputElement): void {
    const datoteka = birac.files?.[0];
    birac.value = ''; // da se ista datoteka može odabrati ponovno nakon izmjene
    if (!datoteka) {
      return;
    }
    if (datoteka.size > 2 * 1024 * 1024) {
      this.poruka('Predložak smije imati najviše 2 MB.');
      return;
    }
    this.api.ucitajPredlozakUgovora(datoteka).subscribe({
      next: (i) => {
        this.info.set(i);
        this.poruka('Predložak je učitan. Novi ugovori koriste njega.');
      },
      error: (err) => this.poruka(porukaGreske(err)),
    });
  }

  vratiZadani(): void {
    if (!confirm('Vratiti zadani predložak? Vaš predložak će se obrisati iz aplikacije.')) {
      return;
    }
    this.api.vratiZadaniPredlozak().subscribe({
      next: (i) => {
        this.info.set(i);
        this.poruka('Vraćen je zadani predložak.');
      },
      error: (err) => this.poruka(porukaGreske(err)),
    });
  }

  private poruka(tekst: string): void {
    this.snack.open(tekst, 'OK', { duration: 5000 });
  }
}
