import { Component, inject, OnInit, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSnackBar } from '@angular/material/snack-bar';
import { ApiService } from '../core/api.service';
import { AuthService } from '../core/auth.service';
import { ClanTvrtke, PodaciTvrtke, Tvrtka } from '../core/modeli';
import { porukaGreske } from '../core/greska';

@Component({
  selector: 'app-tvrtka',
  imports: [FormsModule, MatCardModule, MatButtonModule, MatIconModule, MatFormFieldModule, MatInputModule],
  template: `
    @if (tvrtka(); as t) {
      <mat-card appearance="outlined">
        <mat-card-header>
          <mat-card-title>Podaci tvrtke</mat-card-title>
          <mat-card-subtitle>idu u zaglavlje ugovora</mat-card-subtitle>
        </mat-card-header>
        <mat-card-content>
          @if (auth.vlasnik()) {
            <form (ngSubmit)="spremi()" #f="ngForm" class="podaci">
              <mat-form-field appearance="outline" class="cijelo">
                <mat-label>Naziv</mat-label>
                <input matInput name="naziv" [(ngModel)]="podaci.naziv" required>
              </mat-form-field>
              <mat-form-field appearance="outline" class="cijelo">
                <mat-label>Adresa</mat-label>
                <input matInput name="adresa" [(ngModel)]="podaci.adresa" placeholder="npr. Ulica 1, Đakovo">
              </mat-form-field>
              <mat-form-field appearance="outline">
                <mat-label>OIB</mat-label>
                <input matInput name="oib" [(ngModel)]="podaci.oib" inputmode="numeric" maxlength="11" pattern="\d{11}">
                <mat-error>OIB ima 11 znamenki</mat-error>
              </mat-form-field>
              <mat-form-field appearance="outline">
                <mat-label>Telefon</mat-label>
                <input matInput name="telefon" [(ngModel)]="podaci.telefon" type="tel">
              </mat-form-field>
              <mat-form-field appearance="outline" class="cijelo">
                <mat-label>Email</mat-label>
                <input matInput name="email" [(ngModel)]="podaci.email" type="email" email>
                <mat-error>Email nije ispravan</mat-error>
              </mat-form-field>
              <div class="cijelo"><button mat-flat-button type="submit" [disabled]="f.invalid || !podaci.naziv.trim()">Spremi</button></div>
            </form>
          } @else {
            <p><strong>{{ t.naziv }}</strong></p>
            @if (t.adresa) { <p>{{ t.adresa }}</p> }
            @if (t.oib) { <p>OIB: {{ t.oib }}</p> }
            @if (t.telefon) { <p>{{ t.telefon }}</p> }
            @if (t.email) { <p>{{ t.email }}</p> }
          }
        </mat-card-content>
      </mat-card>

      <mat-card appearance="outlined">
        <mat-card-header><mat-card-title>Članovi</mat-card-title></mat-card-header>
        <mat-card-content>
          @for (c of t.clanovi; track c.id) {
            <div class="red">
              <div class="glavno">
                <strong>{{ c.ime }}</strong>
                @if (c.uloga === 'VLASNIK') { <span class="sivo"> · vlasnik</span> }
                <div class="sivo">{{ c.email }}</div>
              </div>
              @if (auth.vlasnik() && c.uloga !== 'VLASNIK') {
                <button mat-icon-button (click)="ukloni(c)" aria-label="Ukloni člana"><mat-icon>person_remove</mat-icon></button>
              }
            </div>
          }
        </mat-card-content>
      </mat-card>

      @if (auth.vlasnik()) {
        <mat-card appearance="outlined">
          <mat-card-header><mat-card-title>Pozovi člana</mat-card-title></mat-card-header>
          <mat-card-content>
            <p class="sivo">Osoba dobiva email sa šifrom i pridružuje se na stranici „Pridruži se tvrtki”.</p>
            <form (ngSubmit)="pozovi()" class="u-redu">
              <mat-form-field appearance="outline" subscriptSizing="dynamic">
                <mat-label>Email</mat-label>
                <input matInput type="email" name="email" [(ngModel)]="emailPoziva" required email>
              </mat-form-field>
              <button mat-flat-button type="submit" [disabled]="!emailPoziva.includes('@')">Pošalji poziv</button>
            </form>
          </mat-card-content>
        </mat-card>
      }
    }
  `,
  styles: `
    :host { display: flex; flex-direction: column; gap: 16px; }
    .podaci { display: grid; grid-template-columns: 1fr 1fr; gap: 0 12px; margin-top: 8px; }
    .cijelo { grid-column: 1 / -1; }
    @media (max-width: 480px) { .podaci { grid-template-columns: 1fr; } }
    mat-card-content p { margin: 4px 0; }
    .u-redu { display: flex; gap: 8px; align-items: center; flex-wrap: wrap; }
    .u-redu mat-form-field { flex: 1; min-width: 220px; }
  `,
})
export class TvrtkaComponent implements OnInit {
  private api = inject(ApiService);
  private snack = inject(MatSnackBar);
  auth = inject(AuthService);

  tvrtka = signal<Tvrtka | null>(null);
  podaci: PodaciTvrtke = { naziv: '', adresa: null, oib: null, telefon: null, email: null };
  emailPoziva = '';

  ngOnInit(): void {
    this.ucitaj();
  }

  spremi(): void {
    // Prazno polje šalje se kao null, da backend ne dobije "" umjesto „nije upisano”
    const prazno = (s: string | null) => (s?.trim() ? s.trim() : null);
    const p = this.podaci;
    this.api.spremiTvrtku({ naziv: p.naziv.trim(), adresa: prazno(p.adresa), oib: prazno(p.oib),
      telefon: prazno(p.telefon), email: prazno(p.email) }).subscribe({
      next: (t) => {
        this.postavi(t);
        this.auth.osvjeziKorisnika();
        this.poruka('Podaci su spremljeni.');
      },
      error: (err) => this.poruka(porukaGreske(err)),
    });
  }

  pozovi(): void {
    this.api.pozovi(this.emailPoziva.trim()).subscribe({
      next: (p) => {
        this.emailPoziva = '';
        this.poruka(p.poruka);
      },
      error: (err) => this.poruka(porukaGreske(err)),
    });
  }

  ukloni(c: ClanTvrtke): void {
    if (!confirm(`Ukloniti ${c.ime} iz tvrtke?`)) {
      return;
    }
    this.api.ukloniClana(c.id).subscribe({
      next: () => this.ucitaj(),
      error: (err) => this.poruka(porukaGreske(err)),
    });
  }

  private ucitaj(): void {
    this.api.tvrtka().subscribe((t) => this.postavi(t));
  }

  private postavi(t: Tvrtka): void {
    this.tvrtka.set(t);
    this.podaci = { naziv: t.naziv, adresa: t.adresa, oib: t.oib, telefon: t.telefon, email: t.email };
  }

  private poruka(tekst: string): void {
    this.snack.open(tekst, 'OK', { duration: 4000 });
  }
}
