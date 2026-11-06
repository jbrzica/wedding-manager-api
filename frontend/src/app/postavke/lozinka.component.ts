import { Component, inject, signal } from '@angular/core';
import { AbstractControl, FormBuilder, ReactiveFormsModule, ValidationErrors, Validators } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSnackBar } from '@angular/material/snack-bar';
import { ApiService } from '../core/api.service';
import { porukaGreske } from '../core/greska';

function lozinkeIste(grupa: AbstractControl): ValidationErrors | null {
  const nova = grupa.get('novaLozinka')?.value;
  const ponovljena = grupa.get('ponovljena')?.value;
  return nova && ponovljena && nova !== ponovljena ? { razlicite: true } : null;
}

@Component({
  selector: 'app-lozinka',
  imports: [ReactiveFormsModule, MatCardModule, MatButtonModule, MatFormFieldModule, MatInputModule],
  template: `
    <mat-card appearance="outlined">
      <mat-card-header><mat-card-title>Promjena lozinke</mat-card-title></mat-card-header>
      <mat-card-content>
        <form [formGroup]="forma" (ngSubmit)="spremi()" class="stupac">
          <mat-form-field appearance="outline">
            <mat-label>Trenutna lozinka</mat-label>
            <input matInput type="password" formControlName="staraLozinka" autocomplete="current-password">
          </mat-form-field>
          <mat-form-field appearance="outline">
            <mat-label>Nova lozinka</mat-label>
            <input matInput type="password" formControlName="novaLozinka" autocomplete="new-password">
            <mat-error>Barem 8 znakova</mat-error>
          </mat-form-field>
          <mat-form-field appearance="outline">
            <mat-label>Ponovi novu lozinku</mat-label>
            <input matInput type="password" formControlName="ponovljena" autocomplete="new-password">
            @if (forma.hasError('razlicite')) { <mat-hint class="greska">Lozinke se ne podudaraju</mat-hint> }
          </mat-form-field>
          <div>
            <button mat-flat-button type="submit" [disabled]="forma.invalid || sprema()">Promijeni lozinku</button>
          </div>
        </form>
      </mat-card-content>
    </mat-card>
  `,
  styles: `
    .stupac { display: flex; flex-direction: column; gap: 4px; max-width: 360px; }
    .greska { color: var(--mat-sys-error); }
  `,
})
export class LozinkaComponent {
  private api = inject(ApiService);
  private snack = inject(MatSnackBar);

  sprema = signal(false);
  forma = inject(FormBuilder).nonNullable.group({
    staraLozinka: ['', Validators.required],
    novaLozinka: ['', [Validators.required, Validators.minLength(8)]],
    ponovljena: ['', Validators.required],
  }, { validators: lozinkeIste });

  spremi(): void {
    const { staraLozinka, novaLozinka } = this.forma.getRawValue();
    this.sprema.set(true);
    this.api.promjenaLozinke(staraLozinka, novaLozinka).subscribe({
      next: (p) => {
        this.sprema.set(false);
        this.forma.reset();
        this.snack.open(p.poruka, 'OK', { duration: 4000 });
      },
      error: (err) => {
        this.sprema.set(false);
        this.snack.open(porukaGreske(err), 'OK', { duration: 4000 });
      },
    });
  }
}
