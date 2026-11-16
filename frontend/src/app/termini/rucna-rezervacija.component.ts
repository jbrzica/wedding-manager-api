import { Component, computed, inject, signal } from '@angular/core';
import { CurrencyPipe, DatePipe } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { toSignal } from '@angular/core/rxjs-interop';
import { DogadjajDetalji, TerminiApi } from './termini-api';
import { izIso } from '../core/datumi';
import { porukaGreske } from '../core/greska';

export interface RucnaPodaci {
  dogadjaj: DogadjajDetalji;
  datum?: string;
  vrijeme?: string;
}

// Za nekoga tko je nazvao: tata upiše termin sam, bez maila klijentu
@Component({
  selector: 'app-rucna-rezervacija',
  imports: [CurrencyPipe, DatePipe, ReactiveFormsModule, MatDialogModule, MatButtonModule, MatFormFieldModule,
    MatInputModule, MatSelectModule],
  template: `
    <h2 mat-dialog-title>Dodaj termin</h2>
    <mat-dialog-content>
      <form [formGroup]="forma" (ngSubmit)="spremi()" id="rucna" class="forma">
        <mat-form-field appearance="outline" subscriptSizing="dynamic">
          <mat-label>Paket</mat-label>
          <mat-select formControlName="paketId">
            @for (p of podaci.dogadjaj.paketi; track p.id) {
              <mat-option [value]="p.id">{{ p.naziv }} · {{ p.trajanjeMinuta }} min · {{ p.cijena | currency: 'EUR' : 'symbol' : '1.0-2' }}</mat-option>
            }
          </mat-select>
        </mat-form-field>
        <div class="red">
          <mat-form-field appearance="outline" subscriptSizing="dynamic">
            <mat-label>Dan</mat-label>
            <mat-select formControlName="datum">
              @for (d of podaci.dogadjaj.raspored; track d.datum) {
                <mat-option [value]="d.datum">{{ izIso(d.datum) | date: 'EEE d. M.' }}</mat-option>
              }
            </mat-select>
          </mat-form-field>
          <mat-form-field appearance="outline" subscriptSizing="dynamic">
            <mat-label>Vrijeme</mat-label>
            <mat-select formControlName="vrijeme">
              @for (v of slobodna(); track v) { <mat-option [value]="v">{{ v.slice(0, 5) }}</mat-option> }
            </mat-select>
          </mat-form-field>
        </div>
        <mat-form-field appearance="outline" subscriptSizing="dynamic">
          <mat-label>Ime i prezime</mat-label>
          <input matInput formControlName="ime">
        </mat-form-field>
        <mat-form-field appearance="outline" subscriptSizing="dynamic">
          <mat-label>Telefon</mat-label>
          <input matInput formControlName="telefon" type="tel" inputmode="tel">
        </mat-form-field>
        <mat-form-field appearance="outline" subscriptSizing="dynamic">
          <mat-label>Napomena</mat-label>
          <input matInput formControlName="napomena">
        </mat-form-field>
        @if (greska()) { <p class="greska">{{ greska() }}</p> }
      </form>
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-button mat-dialog-close type="button">Odustani</button>
      <button mat-flat-button type="submit" form="rucna" [disabled]="forma.invalid || sprema()">Spremi</button>
    </mat-dialog-actions>
  `,
  styles: `
    .forma { display: flex; flex-direction: column; gap: 12px; padding-top: 6px; }
    .red { display: grid; grid-template-columns: 1fr 1fr; gap: 10px; }
    .greska { color: var(--mat-sys-error); margin: 0; }
  `,
})
export class RucnaRezervacijaComponent {
  podaci = inject<RucnaPodaci>(MAT_DIALOG_DATA);
  private api = inject(TerminiApi);
  private dialogRef = inject(MatDialogRef<RucnaRezervacijaComponent, boolean>);

  readonly izIso = izIso;
  sprema = signal(false);
  greska = signal('');

  forma = inject(FormBuilder).group({
    paketId: [this.podaci.dogadjaj.paketi[0]?.id ?? null, Validators.required],
    datum: [this.podaci.datum ?? this.podaci.dogadjaj.raspored[0]?.datum ?? null, Validators.required],
    vrijeme: [this.podaci.vrijeme ?? null as string | null, Validators.required],
    ime: ['', [Validators.required, Validators.maxLength(200)]],
    telefon: ['', Validators.maxLength(50)],
    napomena: ['', Validators.maxLength(1000)],
  });

  private datum = toSignal(this.forma.controls.datum.valueChanges, { initialValue: this.forma.controls.datum.value });
  slobodna = computed(() => this.podaci.dogadjaj.raspored.find((d) => d.datum === this.datum())
    ?.mjesta.filter((m) => !m.rezervacija).map((m) => m.vrijeme) ?? []);

  spremi(): void {
    if (this.forma.invalid || this.sprema()) {
      return;
    }
    const f = this.forma.getRawValue();
    this.sprema.set(true);
    this.api.rucnaRezervacija(this.podaci.dogadjaj.id, {
      paketId: f.paketId!, datum: f.datum!, vrijeme: f.vrijeme!, ime: f.ime!.trim(),
      telefon: f.telefon || null, napomena: f.napomena || null,
    }).subscribe({
      next: () => this.dialogRef.close(true),
      error: (err) => { this.greska.set(porukaGreske(err)); this.sprema.set(false); },
    });
  }
}
