import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { AuthService } from '../core/auth.service';
import { porukaGreske } from '../core/greska';

@Component({
  selector: 'app-prijava',
  imports: [ReactiveFormsModule, RouterLink, MatCardModule, MatFormFieldModule, MatInputModule, MatButtonModule],
  template: `
    <div class="auth-stranica">
      <mat-card class="auth-kartica" appearance="outlined">
        <h1>Prijava</h1>
        <p class="podnaslov">Wedding Manager za fotografe i snimatelje</p>

        <form [formGroup]="forma" (ngSubmit)="posalji()">
          <mat-form-field appearance="outline">
            <mat-label>Email</mat-label>
            <input matInput type="email" formControlName="email" autocomplete="email">
          </mat-form-field>
          <mat-form-field appearance="outline">
            <mat-label>Lozinka</mat-label>
            <input matInput type="password" formControlName="lozinka" autocomplete="current-password">
          </mat-form-field>

          @if (greska()) {
            <p class="greska">{{ greska() }}</p>
          }

          <button mat-flat-button type="submit" [disabled]="forma.invalid || salje()">Prijavi se</button>
        </form>

        <p class="dno">Nemate račun? <a routerLink="/registracija">Registrirajte tvrtku</a></p>
        <p class="dno">Dobili ste poziv? <a routerLink="/poziv">Pridružite se tvrtki</a></p>
      </mat-card>
    </div>
  `,
})
export class PrijavaComponent {
  private auth = inject(AuthService);
  private router = inject(Router);

  forma = inject(FormBuilder).nonNullable.group({
    email: ['', [Validators.required, Validators.email]],
    lozinka: ['', Validators.required],
  });
  greska = signal('');
  salje = signal(false);

  posalji(): void {
    const { email, lozinka } = this.forma.getRawValue();
    this.salje.set(true);
    this.greska.set('');
    this.auth.prijava(email, lozinka).subscribe({
      next: () => this.router.navigate(['/']),
      error: (err: HttpErrorResponse) => {
        this.salje.set(false);
        // Email nije potvrđen: šaljemo na unos šifre
        if (err.status === 403) {
          this.router.navigate(['/potvrda'], { queryParams: { email } });
          return;
        }
        this.greska.set(porukaGreske(err));
      },
    });
  }
}
