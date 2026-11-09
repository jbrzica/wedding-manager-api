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
import { Paket } from '../core/modeli';
import { porukaGreske } from '../core/greska';

@Component({
  selector: 'app-paketi',
  imports: [CurrencyPipe, ReactiveFormsModule, MatCardModule, MatButtonModule, MatIconModule, MatFormFieldModule, MatInputModule],
  template: `
    <p class="opis">Vaši paketi i cijene. Kod unosa vjenčanja odabirom paketa automatski se upisuje cijena.</p>

    <mat-card appearance="outlined" class="lista">
      @for (p of paketi(); track p.id) {
        <div class="red">
          <div class="glavno">
            <strong>{{ p.naziv }}</strong>
            @if (p.opis) { <div class="sivo">{{ p.opis }}</div> }
          </div>
          <span class="cijena">{{ p.cijena | currency: 'EUR' }}</span>
          <button mat-icon-button (click)="uredi(p)" aria-label="Uredi"><mat-icon>edit</mat-icon></button>
          <button mat-icon-button (click)="obrisi(p)" aria-label="Obriši"><mat-icon>delete_outline</mat-icon></button>
        </div>
      } @empty {
        <p class="prazno">Još nemaš paketa. Upiši npr. „Cijeli dan“ ili „Samo crkva“ s cijenom; kad ga odabereš na vjenčanju, cijena se upiše sama.</p>
      }
    </mat-card>

    <mat-card appearance="outlined">
      <mat-card-header><mat-card-title>{{ urediId() ? 'Uredi paket' : 'Novi paket' }}</mat-card-title></mat-card-header>
      <mat-card-content>
        <form [formGroup]="forma" (ngSubmit)="spremi()" class="forma">
          <mat-form-field appearance="outline">
            <mat-label>Naziv</mat-label>
            <input matInput formControlName="naziv" placeholder="npr. Foto cijeli dan">
          </mat-form-field>
          <mat-form-field appearance="outline">
            <mat-label>Cijena (€)</mat-label>
            <input matInput type="number" min="0" formControlName="cijena">
          </mat-form-field>
          <mat-form-field appearance="outline" class="cijeli-red">
            <mat-label>Opis</mat-label>
            <input matInput formControlName="opis" placeholder="Što je uključeno">
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
export class PaketiComponent implements OnInit {
  private api = inject(ApiService);
  private snack = inject(MatSnackBar);

  paketi = signal<Paket[]>([]);
  urediId = signal<number | null>(null);
  forma = inject(FormBuilder).group({
    naziv: ['', Validators.required],
    cijena: [null as number | null, [Validators.required, Validators.min(0)]],
    opis: [''],
  });

  ngOnInit(): void {
    this.ucitaj();
  }

  uredi(p: Paket): void {
    this.urediId.set(p.id);
    this.forma.setValue({ naziv: p.naziv, cijena: p.cijena, opis: p.opis ?? '' });
  }

  odustani(): void {
    this.urediId.set(null);
    this.forma.reset();
  }

  spremi(): void {
    const v = this.forma.getRawValue();
    this.api.spremiPaket({ naziv: v.naziv!, cijena: v.cijena!, opis: v.opis || null }, this.urediId() ?? undefined).subscribe({
      next: () => {
        this.odustani();
        this.ucitaj();
      },
      error: (err) => this.snack.open(porukaGreske(err), 'OK', { duration: 4000 }),
    });
  }

  obrisi(p: Paket): void {
    if (!confirm(`Obrisati paket "${p.naziv}"?`)) {
      return;
    }
    this.api.obrisiPaket(p.id).subscribe({
      next: () => this.ucitaj(),
      error: (err) => this.snack.open(porukaGreske(err), 'OK', { duration: 5000 }),
    });
  }

  private ucitaj(): void {
    this.api.paketi().subscribe((l) => this.paketi.set(l));
  }
}
