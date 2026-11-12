import { Component, inject, input, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { AuthService } from '../core/auth.service';
import { porukaGreske } from '../core/greska';

@Component({
  selector: 'app-potvrda',
  imports: [FormsModule, RouterLink, MatCardModule, MatFormFieldModule, MatInputModule, MatButtonModule],
  template: `
    <div class="auth-stranica">
      <mat-card class="auth-kartica" appearance="outlined">
        <h1>Potvrdi email</h1>
        <p class="podnaslov">Poslali smo šesteroznamenkastu šifru na <strong>{{ email() }}</strong>.</p>

        <form (ngSubmit)="potvrdi()">
          <mat-form-field appearance="outline">
            <mat-label>Šifra</mat-label>
            <input matInput name="kod" [(ngModel)]="kod" inputmode="numeric" maxlength="6"
                   autocomplete="one-time-code" class="sifra">
          </mat-form-field>

          @if (greska()) {
            <p class="greska">{{ greska() }}</p>
          }
          @if (info()) {
            <p class="info">{{ info() }}</p>
          }

          <button mat-flat-button type="submit" [disabled]="kod.length !== 6 || salje()">Potvrdi</button>
          <button mat-button type="button" (click)="ponovo()" [disabled]="salje()">Pošalji novu šifru</button>
        </form>

        <p class="dno"><a routerLink="/prijava">Natrag na prijavu</a></p>
      </mat-card>
    </div>
  `,
})
export class PotvrdaComponent {
  private auth = inject(AuthService);
  private router = inject(Router);

  // Dolazi iz adrese: /potvrda?email=...
  email = input('');
  kod = '';
  greska = signal('');
  info = signal('');
  salje = signal(false);

  potvrdi(): void {
    this.salje.set(true);
    this.greska.set('');
    this.auth.potvrdiEmail(this.email(), this.kod).subscribe({
      next: () => this.router.navigate(['/']),
      error: (err) => {
        this.salje.set(false);
        this.greska.set(porukaGreske(err));
      },
    });
  }

  ponovo(): void {
    this.greska.set('');
    this.auth.ponovnoPosaljiKod(this.email()).subscribe({
      next: (p) => this.info.set(p.poruka),
      error: (err) => this.greska.set(porukaGreske(err)),
    });
  }
}
