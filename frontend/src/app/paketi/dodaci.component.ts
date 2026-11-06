import { Component, inject, OnInit, signal } from '@angular/core';
import { CurrencyPipe } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSnackBar } from '@angular/material/snack-bar';
import { ApiService } from '../core/api.service';
import { Dodatak } from '../core/modeli';
import { porukaGreske } from '../core/greska';

@Component({
  selector: 'app-dodaci',
  imports: [CurrencyPipe, ReactiveFormsModule, MatCardModule, MatButtonModule, MatIconModule, MatFormFieldModule, MatInputModule],
  template: `
    <p class="opis">Dodatne usluge koje se naplaćuju uz paket (dron, album, druga kamera...).</p>

    <mat-card appearance="outlined" class="lista">
      @for (p of dodaci(); track p.id) {
        <div class="red">
          <div class="glavno">
            <strong>{{ p.naziv }}</strong>
          </div>
          <span class="cijena">{{ p.cijena | currency: 'EUR' }}</span>
          <button mat-icon-button (click)="uredi(p)" aria-label="Uredi"><mat-icon>edit</mat-icon></button>
          <button mat-icon-button (click)="obrisi(p)" aria-label="Obriši"><mat-icon>delete_outline</mat-icon></button>
        </div>
      } @empty {
        <p class="prazno">Još nema dodataka.</p>
      }
    </mat-card>

    <mat-card appearance="outlined">
      <mat-card-header><mat-card-title>{{ urediId() ? 'Uredi dodatak' : 'Novi dodatak' }}</mat-card-title></mat-card-header>
      <mat-card-content>
        <form [formGroup]="forma" (ngSubmit)="spremi()" class="forma">
          <mat-form-field appearance="outline">
            <mat-label>Naziv</mat-label>
            <input matInput formControlName="naziv" placeholder="npr. Dron">
          </mat-form-field>
          <mat-form-field appearance="outline">
            <mat-label>Cijena (€)</mat-label>
            <input matInput type="number" min="0" formControlName="cijena">
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
export class DodaciComponent implements OnInit {
  private api = inject(ApiService);
  private snack = inject(MatSnackBar);

  dodaci = signal<Dodatak[]>([]);
  urediId = signal<number | null>(null);
  forma = inject(FormBuilder).group({
    naziv: ['', Validators.required],
    cijena: [null as number | null, [Validators.required, Validators.min(0)]],
  });

  ngOnInit(): void {
    this.ucitaj();
  }

  uredi(p: Dodatak): void {
    this.urediId.set(p.id);
    this.forma.setValue({ naziv: p.naziv, cijena: p.cijena });
  }

  odustani(): void {
    this.urediId.set(null);
    this.forma.reset();
  }

  spremi(): void {
    const v = this.forma.getRawValue();
    this.api.spremiDodatak({ naziv: v.naziv!, cijena: v.cijena! }, this.urediId() ?? undefined).subscribe({
      next: () => {
        this.odustani();
        this.ucitaj();
      },
      error: (err) => this.snack.open(porukaGreske(err), 'OK', { duration: 4000 }),
    });
  }

  obrisi(p: Dodatak): void {
    if (!confirm(`Obrisati dodatak "${p.naziv}"?`)) {
      return;
    }
    this.api.obrisiDodatak(p.id).subscribe({
      next: () => this.ucitaj(),
      error: (err) => this.snack.open(porukaGreske(err), 'OK', { duration: 5000 }),
    });
  }

  private ucitaj(): void {
    this.api.dodaci().subscribe((l) => this.dodaci.set(l));
  }
}
