import { Component, inject, OnInit, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatMenuModule } from '@angular/material/menu';
import { MatSnackBar } from '@angular/material/snack-bar';
import { ApiService } from '../core/api.service';
import { Suradnik, UlogaSuradnika } from '../core/modeli';
import { ULOGE_SURADNIKA, kljucevi } from '../core/oznake';
import { porukaGreske } from '../core/greska';

@Component({
  selector: 'app-suradnici',
  imports: [ReactiveFormsModule, MatCardModule, MatButtonModule, MatIconModule, MatFormFieldModule, MatInputModule, MatSelectModule, MatMenuModule],
  template: `
    <p class="opis">
      Vanjski snimatelji, fotografi, asistenti... Dodajete ih u ekipu pojedinog vjenčanja.
      Svaki suradnik ima svoj link (<mat-icon inline>link</mat-icon>) na kojem vidi samo svoja vjenčanja, bez cijena.
    </p>

    <mat-card appearance="outlined" class="lista">
      @for (s of suradnici(); track s.id) {
        <div class="red">
          <div class="glavno">
            <strong>{{ s.ime }}</strong> <span class="sivo">· {{ oznake[s.uloga] }}</span>
            <div class="sivo">
              @if (s.telefon) { <a [href]="'tel:' + s.telefon">{{ s.telefon }}</a> }
              @if (s.email) { · {{ s.email }} }
            </div>
          </div>
          <button mat-icon-button [matMenuTriggerFor]="izbornikLinka" aria-label="Link za suradnika"><mat-icon>link</mat-icon></button>
          <mat-menu #izbornikLinka="matMenu">
            <button mat-menu-item (click)="posaljiLink(s)"><mat-icon>send</mat-icon> Pošalji link suradniku</button>
            <button mat-menu-item (click)="kopirajLink(s)"><mat-icon>content_copy</mat-icon> Kopiraj link</button>
            <button mat-menu-item (click)="noviLink(s)"><mat-icon>refresh</mat-icon> Napravi novi link</button>
          </mat-menu>
          <button mat-icon-button (click)="uredi(s)" aria-label="Uredi"><mat-icon>edit</mat-icon></button>
          <button mat-icon-button (click)="obrisi(s)" aria-label="Obriši"><mat-icon>delete_outline</mat-icon></button>
        </div>
      } @empty {
        <p class="prazno">Još nemaš suradnika. Dodaj snimatelje, fotografe ili asistente s kojima radiš, pa ih možeš stavljati u ekipu vjenčanja.</p>
      }
    </mat-card>

    <mat-card appearance="outlined">
      <mat-card-header><mat-card-title>{{ urediId() ? 'Uredi suradnika' : 'Novi suradnik' }}</mat-card-title></mat-card-header>
      <mat-card-content>
        <form [formGroup]="forma" (ngSubmit)="spremi()" class="forma">
          <mat-form-field appearance="outline">
            <mat-label>Ime</mat-label>
            <input matInput formControlName="ime">
          </mat-form-field>
          <mat-form-field appearance="outline">
            <mat-label>Uloga</mat-label>
            <mat-select formControlName="uloga">
              @for (u of uloge; track u) { <mat-option [value]="u">{{ oznake[u] }}</mat-option> }
            </mat-select>
          </mat-form-field>
          <mat-form-field appearance="outline">
            <mat-label>Telefon</mat-label>
            <input matInput type="tel" formControlName="telefon">
          </mat-form-field>
          <mat-form-field appearance="outline">
            <mat-label>Email</mat-label>
            <input matInput type="email" formControlName="email">
          </mat-form-field>
          <mat-form-field appearance="outline" class="cijeli-red">
            <mat-label>Napomena</mat-label>
            <input matInput formControlName="napomena">
          </mat-form-field>
          <div class="gumbi">
            @if (urediId()) { <button mat-button type="button" (click)="odustani()">Odustani</button> }
            <button mat-flat-button type="submit" [disabled]="forma.invalid">Spremi</button>
          </div>
        </form>
      </mat-card-content>
    </mat-card>
  `,
})
export class SuradniciComponent implements OnInit {
  private api = inject(ApiService);
  private snack = inject(MatSnackBar);

  readonly uloge = kljucevi(ULOGE_SURADNIKA);
  readonly oznake = ULOGE_SURADNIKA;
  suradnici = signal<Suradnik[]>([]);
  urediId = signal<number | null>(null);
  forma = inject(FormBuilder).group({
    ime: ['', Validators.required],
    uloga: ['VIDEOGRAF' as UlogaSuradnika, Validators.required],
    telefon: [''],
    email: ['', Validators.email],
    napomena: [''],
  });

  ngOnInit(): void {
    this.ucitaj();
  }

  uredi(s: Suradnik): void {
    this.urediId.set(s.id);
    this.forma.setValue({ ime: s.ime, uloga: s.uloga, telefon: s.telefon ?? '', email: s.email ?? '', napomena: s.napomena ?? '' });
  }

  odustani(): void {
    this.urediId.set(null);
    this.forma.reset();
  }

  spremi(): void {
    const v = this.forma.getRawValue();
    const suradnik = {
      ime: v.ime!, uloga: v.uloga!, telefon: v.telefon || null, email: v.email || null, napomena: v.napomena || null,
    };
    this.api.spremiSuradnika(suradnik, this.urediId() ?? undefined).subscribe({
      next: () => {
        this.odustani();
        this.ucitaj();
      },
      error: (err) => this.snack.open(porukaGreske(err), 'OK', { duration: 4000 }),
    });
  }

  obrisi(s: Suradnik): void {
    if (!confirm(`Obrisati suradnika "${s.ime}"?`)) {
      return;
    }
    this.api.obrisiSuradnika(s.id).subscribe({
      next: () => this.ucitaj(),
      error: (err) => this.snack.open(porukaGreske(err), 'OK', { duration: 5000 }),
    });
  }

  // Na mobitelu otvara WhatsApp/Viber; na računalu link se kopira
  posaljiLink(s: Suradnik): void {
    this.api.suradnikLink(s.id).subscribe({
      next: async (r) => {
        const url = window.location.origin + r.putanja;
        const tekst = `Bok ${s.ime}, ovdje su ti sva vjenčanja na kojima radiš s nama (raspored, adrese i kontakti). Stranica je uvijek ažurna:\n${url}`;
        if (navigator.share) {
          try {
            await navigator.share({ text: tekst });
            return;
          } catch {
            // korisnik je odustao ili dijeljenje nije dostupno, pa kopiramo
          }
        }
        this.kopiraj(tekst, 'Poruka s linkom je kopirana, zalijepite je suradniku.');
      },
      error: (err) => this.poruka(porukaGreske(err)),
    });
  }

  kopirajLink(s: Suradnik): void {
    this.api.suradnikLink(s.id).subscribe({
      next: (r) => this.kopiraj(window.location.origin + r.putanja, 'Link je kopiran.'),
      error: (err) => this.poruka(porukaGreske(err)),
    });
  }

  noviLink(s: Suradnik): void {
    if (!confirm(`Stari link za "${s.ime}" prestat će raditi. Novi link morate mu ponovno poslati. Nastaviti?`)) {
      return;
    }
    this.api.noviSuradnikLink(s.id).subscribe({
      next: (r) => this.kopiraj(window.location.origin + r.putanja, 'Napravljen je novi link i kopiran je.'),
      error: (err) => this.poruka(porukaGreske(err)),
    });
  }

  private kopiraj(tekst: string, uspjeh: string): void {
    navigator.clipboard.writeText(tekst).then(
      () => this.poruka(uspjeh),
      () => this.poruka('Kopiranje nije uspjelo.'),
    );
  }

  private poruka(tekst: string): void {
    this.snack.open(tekst, 'OK', { duration: 4000 });
  }

  private ucitaj(): void {
    this.api.suradnici().subscribe((l) => this.suradnici.set(l));
  }
}
