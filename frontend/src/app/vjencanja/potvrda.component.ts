import { Component, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { uIso } from '../core/datumi';

export interface PotvrdaPodaci {
  imena: string;
  imaCijenu: boolean;
}

export interface PotvrdaOdabir {
  kapara: number | null;
  datum: string;
  ugovor: boolean;
}

/**
 * Par je rekao "može": jedan prozor umjesto tri odvojena koraka (status, kapara, ugovor).
 * Prozor samo pita; spremanje radi stranica vjenčanja koja ga je otvorila.
 */
@Component({
  selector: 'app-potvrda',
  imports: [ReactiveFormsModule, MatDialogModule, MatButtonModule, MatCheckboxModule, MatFormFieldModule, MatInputModule,
    MatDatepickerModule],
  template: `
    <h2 mat-dialog-title>Potvrdi vjenčanje</h2>
    <mat-dialog-content>
      <p class="tko">{{ podaci.imena }}</p>
      <form [formGroup]="forma" (ngSubmit)="potvrdi()" id="potvrda">
        <mat-form-field appearance="outline" subscriptSizing="dynamic">
          <mat-label>Kapara (€)</mat-label>
          <input matInput type="number" min="0" step="0.01" inputmode="decimal" formControlName="kapara">
          <mat-hint>Ostavi prazno ako kapara još nije plaćena</mat-hint>
        </mat-form-field>
        <mat-form-field appearance="outline" subscriptSizing="dynamic">
          <mat-label>Datum uplate</mat-label>
          <input matInput [matDatepicker]="birac" formControlName="datum">
          <mat-datepicker-toggle matIconSuffix [for]="birac" />
          <mat-datepicker #birac />
        </mat-form-field>
        <mat-checkbox formControlName="ugovor">Odmah napravi ugovor</mat-checkbox>
        @if (!podaci.imaCijenu) {
          <p class="upozorenje">Cijena još nije upisana, pa će u ugovoru biti 0 €. Prvo je upiši pod „Uredi”.</p>
        }
      </form>
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-button mat-dialog-close type="button">Odustani</button>
      <button mat-flat-button type="submit" form="potvrda" [disabled]="forma.invalid">Potvrdi</button>
    </mat-dialog-actions>
  `,
  styles: `
    .tko { margin: 0 0 12px; font-weight: 600; }
    form { display: flex; flex-direction: column; gap: 14px; padding-top: 4px; }
    .upozorenje { margin: 0; padding: 10px 12px; border-radius: 10px; font-size: 14px;
                  background: var(--wm-upozorenje-pozadina); color: var(--wm-upozorenje); }
  `,
})
export class PotvrdaComponent {
  podaci = inject<PotvrdaPodaci>(MAT_DIALOG_DATA);
  private dialogRef = inject(MatDialogRef<PotvrdaComponent, PotvrdaOdabir>);

  forma = inject(FormBuilder).group({
    kapara: [null as number | null, Validators.min(0)],
    datum: [new Date(), Validators.required],
    ugovor: [true],
  });

  potvrdi(): void {
    if (this.forma.invalid) {
      return;
    }
    const f = this.forma.getRawValue();
    this.dialogRef.close({ kapara: f.kapara && f.kapara > 0 ? f.kapara : null, datum: uIso(f.datum!), ugovor: !!f.ugovor });
  }
}
