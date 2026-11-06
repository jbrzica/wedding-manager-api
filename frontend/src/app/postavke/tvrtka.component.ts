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
import { ClanTvrtke, Tvrtka } from '../core/modeli';
import { porukaGreske } from '../core/greska';

@Component({
  selector: 'app-tvrtka',
  imports: [FormsModule, MatCardModule, MatButtonModule, MatIconModule, MatFormFieldModule, MatInputModule],
  template: `
    @if (tvrtka(); as t) {
      <mat-card appearance="outlined">
        <mat-card-header><mat-card-title>Naziv tvrtke</mat-card-title></mat-card-header>
        <mat-card-content>
          @if (auth.vlasnik()) {
            <form (ngSubmit)="preimenuj()" class="u-redu">
              <mat-form-field appearance="outline" subscriptSizing="dynamic">
                <input matInput name="naziv" [(ngModel)]="naziv" required>
              </mat-form-field>
              <button mat-flat-button type="submit" [disabled]="!naziv.trim() || naziv === t.naziv">Spremi</button>
            </form>
          } @else {
            <p>{{ t.naziv }}</p>
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
    .u-redu { display: flex; gap: 8px; align-items: center; flex-wrap: wrap; }
    .u-redu mat-form-field { flex: 1; min-width: 220px; }
  `,
})
export class TvrtkaComponent implements OnInit {
  private api = inject(ApiService);
  private snack = inject(MatSnackBar);
  auth = inject(AuthService);

  tvrtka = signal<Tvrtka | null>(null);
  naziv = '';
  emailPoziva = '';

  ngOnInit(): void {
    this.ucitaj();
  }

  preimenuj(): void {
    this.api.preimenujTvrtku(this.naziv.trim()).subscribe({
      next: (t) => {
        this.postavi(t);
        this.auth.osvjeziKorisnika();
        this.poruka('Naziv je spremljen.');
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
    this.naziv = t.naziv;
  }

  private poruka(tekst: string): void {
    this.snack.open(tekst, 'OK', { duration: 4000 });
  }
}
