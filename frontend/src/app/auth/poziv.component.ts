import { Component, inject, input, OnInit, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { AuthService } from '../core/auth.service';
import { porukaGreske } from '../core/greska';

@Component({
  selector: 'app-poziv',
  imports: [ReactiveFormsModule, RouterLink, MatCardModule, MatFormFieldModule, MatInputModule, MatButtonModule],
  template: `
    <div class="auth-stranica">
      <mat-card class="auth-kartica" appearance="outlined">
        <h1>Pridruži se tvrtki</h1>
        <p class="podnaslov">Upiši email na koji je stigao poziv i šifru iz poziva.</p>

        <form [formGroup]="forma" (ngSubmit)="posalji()">
          <mat-form-field appearance="outline">
            <mat-label>Email</mat-label>
            <input matInput type="email" formControlName="email" autocomplete="email">
          </mat-form-field>
          <mat-form-field appearance="outline">
            <mat-label>Šifra iz poziva</mat-label>
            <input matInput formControlName="kod" inputmode="numeric" maxlength="6" class="sifra">
          </mat-form-field>
          <mat-form-field appearance="outline">
            <mat-label>Tvoje ime</mat-label>
            <input matInput formControlName="ime" autocomplete="name">
          </mat-form-field>
          <mat-form-field appearance="outline">
            <mat-label>Lozinka</mat-label>
            <input matInput type="password" formControlName="lozinka" autocomplete="new-password">
            <mat-hint>Barem 8 znakova</mat-hint>
          </mat-form-field>

          @if (greska()) {
            <p class="greska">{{ greska() }}</p>
          }

          <button mat-flat-button type="submit" [disabled]="forma.invalid || salje()">Pridruži se</button>
        </form>

        <p class="dno"><a routerLink="/prijava">Natrag na prijavu</a></p>
      </mat-card>
    </div>
  `,
})
export class PozivComponent implements OnInit {
  private auth = inject(AuthService);
  private router = inject(Router);

  email = input('');
  forma = inject(FormBuilder).nonNullable.group({
    email: ['', [Validators.required, Validators.email]],
    kod: ['', [Validators.required, Validators.pattern(/^\d{6}$/)]],
    ime: ['', Validators.required],
    lozinka: ['', [Validators.required, Validators.minLength(8)]],
  });
  greska = signal('');
  salje = signal(false);

  ngOnInit(): void {
    if (this.email()) {
      this.forma.controls.email.setValue(this.email());
    }
  }

  posalji(): void {
    const v = this.forma.getRawValue();
    this.salje.set(true);
    this.greska.set('');
    this.auth.prihvatiPoziv(v.email, v.kod, v.ime, v.lozinka).subscribe({
      next: () => this.router.navigate(['/']),
      error: (err) => {
        this.salje.set(false);
        this.greska.set(porukaGreske(err));
      },
    });
  }
}
