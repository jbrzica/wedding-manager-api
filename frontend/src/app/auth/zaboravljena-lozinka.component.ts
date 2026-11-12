import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { AuthService } from '../core/auth.service';
import { porukaGreske } from '../core/greska';

// Dva koraka: prvo email, zatim šifra iz maila i nova lozinka
@Component({
  selector: 'app-zaboravljena-lozinka',
  imports: [FormsModule, RouterLink, MatCardModule, MatFormFieldModule, MatInputModule, MatButtonModule],
  template: `
    <div class="auth-stranica">
      <mat-card class="auth-kartica" appearance="outlined">
        <h1>Nova lozinka</h1>

        @if (!poslano()) {
          <p class="podnaslov">Upiši email računa. Poslat ćemo ti šifru za novu lozinku.</p>
          <form (ngSubmit)="posaljiSifru()">
            <mat-form-field appearance="outline">
              <mat-label>Email</mat-label>
              <input matInput type="email" name="email" [(ngModel)]="email" autocomplete="email" required>
            </mat-form-field>
            @if (greska()) { <p class="greska">{{ greska() }}</p> }
            <button mat-flat-button type="submit" [disabled]="!email.includes('@') || salje()">Pošalji šifru</button>
          </form>
        } @else {
          <p class="podnaslov">Ako račun postoji, šifra je poslana na <strong>{{ email }}</strong>.</p>
          <form (ngSubmit)="spremi()">
            <mat-form-field appearance="outline">
              <mat-label>Šifra</mat-label>
              <input matInput name="kod" [(ngModel)]="kod" inputmode="numeric" maxlength="6"
                     autocomplete="one-time-code" class="sifra">
            </mat-form-field>
            <mat-form-field appearance="outline">
              <mat-label>Nova lozinka</mat-label>
              <input matInput type="password" name="lozinka" [(ngModel)]="lozinka" autocomplete="new-password">
              <mat-hint>Barem 8 znakova</mat-hint>
            </mat-form-field>
            @if (greska()) { <p class="greska">{{ greska() }}</p> }
            <button mat-flat-button type="submit" [disabled]="kod.length !== 6 || lozinka.length < 8 || salje()">Spremi lozinku</button>
            <button mat-button type="button" (click)="posaljiSifru()" [disabled]="salje()">Pošalji novu šifru</button>
          </form>
        }

        <p class="dno"><a routerLink="/prijava">Natrag na prijavu</a></p>
      </mat-card>
    </div>
  `,
})
export class ZaboravljenaLozinkaComponent {
  private auth = inject(AuthService);
  private router = inject(Router);

  email = '';
  kod = '';
  lozinka = '';
  poslano = signal(false);
  salje = signal(false);
  greska = signal('');

  posaljiSifru(): void {
    this.salje.set(true);
    this.greska.set('');
    this.auth.zaboravljenaLozinka(this.email.trim()).subscribe({
      next: () => {
        this.salje.set(false);
        this.poslano.set(true);
      },
      error: (err) => {
        this.salje.set(false);
        this.greska.set(porukaGreske(err));
      },
    });
  }

  spremi(): void {
    this.salje.set(true);
    this.greska.set('');
    this.auth.novaLozinka(this.email.trim(), this.kod, this.lozinka).subscribe({
      next: () => this.router.navigate(['/']),
      error: (err) => {
        this.salje.set(false);
        this.greska.set(porukaGreske(err));
      },
    });
  }
}
