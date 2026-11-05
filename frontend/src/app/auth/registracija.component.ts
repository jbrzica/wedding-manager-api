import { Component, inject, signal } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { MatCardModule } from '@angular/material/card';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatButtonModule } from '@angular/material/button';
import { AuthService } from '../core/auth.service';
import { porukaGreske } from '../core/greska';

@Component({
  selector: 'app-registracija',
  imports: [ReactiveFormsModule, RouterLink, MatCardModule, MatFormFieldModule, MatInputModule, MatButtonModule],
  template: `
    <div class="auth-stranica">
      <mat-card class="auth-kartica" appearance="outlined">
        <h1>Nova tvrtka</h1>
        <p class="podnaslov">Vi postajete vlasnik računa i kasnije možete pozvati ostale.</p>

        <form [formGroup]="forma" (ngSubmit)="posalji()">
          <mat-form-field appearance="outline">
            <mat-label>Naziv tvrtke</mat-label>
            <input matInput formControlName="nazivTvrtke" placeholder="npr. Foto Video Brzica">
          </mat-form-field>
          <mat-form-field appearance="outline">
            <mat-label>Vaše ime</mat-label>
            <input matInput formControlName="ime" autocomplete="name">
          </mat-form-field>
          <mat-form-field appearance="outline">
            <mat-label>Email</mat-label>
            <input matInput type="email" formControlName="email" autocomplete="email">
          </mat-form-field>
          <mat-form-field appearance="outline">
            <mat-label>Lozinka</mat-label>
            <input matInput type="password" formControlName="lozinka" autocomplete="new-password">
            <mat-hint>Barem 8 znakova</mat-hint>
          </mat-form-field>

          @if (greska()) {
            <p class="greska">{{ greska() }}</p>
          }

          <button mat-flat-button type="submit" [disabled]="forma.invalid || salje()">Registriraj se</button>
        </form>

        <p class="dno">Već imate račun? <a routerLink="/prijava">Prijava</a></p>
      </mat-card>
    </div>
  `,
})
export class RegistracijaComponent {
  private auth = inject(AuthService);
  private router = inject(Router);

  forma = inject(FormBuilder).nonNullable.group({
    nazivTvrtke: ['', Validators.required],
    ime: ['', Validators.required],
    email: ['', [Validators.required, Validators.email]],
    lozinka: ['', [Validators.required, Validators.minLength(8)]],
  });
  greska = signal('');
  salje = signal(false);

  posalji(): void {
    const v = this.forma.getRawValue();
    this.salje.set(true);
    this.greska.set('');
    this.auth.registracija(v.nazivTvrtke, v.ime, v.email, v.lozinka).subscribe({
      next: () => this.router.navigate(['/potvrda'], { queryParams: { email: v.email } }),
      error: (err) => {
        this.salje.set(false);
        this.greska.set(porukaGreske(err));
      },
    });
  }
}
