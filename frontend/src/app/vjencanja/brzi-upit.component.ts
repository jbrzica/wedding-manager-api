import { Component, inject, signal } from '@angular/core';
import { DatePipe } from '@angular/common';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatDatepickerModule } from '@angular/material/datepicker';
import { ApiService } from '../core/api.service';
import { Dostupnost } from '../core/modeli';
import { izIso, uIso } from '../core/datumi';
import { par } from '../core/imena';
import { OPIS_STANJA, stanjeDatuma } from '../core/dostupnost';
import { porukaGreske } from '../core/greska';

/**
 * Upit zapisan dok je mladenka još na telefonu: datum, ime, broj i napomena.
 * Čim se upiše datum vidi se je li slobodan, a sve ostalo se dopuni kad potvrde.
 */
@Component({
  selector: 'app-brzi-upit',
  imports: [DatePipe, ReactiveFormsModule, MatDialogModule, MatButtonModule, MatIconModule, MatFormFieldModule,
    MatInputModule, MatDatepickerModule],
  template: `
    <h2 mat-dialog-title>Brzi upit</h2>
    <mat-dialog-content>
      <form [formGroup]="forma" (ngSubmit)="spremi()" id="brzi-upit">
        <mat-form-field appearance="outline" subscriptSizing="dynamic">
          <mat-label>Datum vjenčanja</mat-label>
          <input matInput [matDatepicker]="birac" formControlName="datum" placeholder="npr. 14.6.2027." (dateChange)="provjeri($event.value)">
          <mat-datepicker-toggle matIconSuffix [for]="birac" />
          <mat-datepicker #birac />
        </mat-form-field>

        @if (dostupnost(); as d) {
          <div [class]="'ishod ' + stanje(d)">
            <mat-icon>{{ opis[stanje(d)].ikona }}</mat-icon>
            <div>
              <strong>{{ opis[stanje(d)].naslov }}</strong> · {{ izIso(d.datum) | date: 'EEEE, d. MMMM yyyy.' }}
              @for (v of d.vjencanja; track v.id) {
                <div>{{ par(v.mlada, v.mladozenja) }}</div>
              }
              @if (d.slobodno && d.blizu.length) {
                <div>U blizini: @for (v of d.blizu; track v.id) { {{ izIso(v.datum) | date: 'EEE d. M.' }}{{ $last ? '' : ', ' }} }</div>
              }
            </div>
          </div>
        }

        <mat-form-field appearance="outline" subscriptSizing="dynamic">
          <mat-label>Tko se javio (ime i prezime)</mat-label>
          <input matInput formControlName="ime" autocomplete="off" autocapitalize="words">
        </mat-form-field>
        <mat-form-field appearance="outline" subscriptSizing="dynamic">
          <mat-label>Telefon</mat-label>
          <input matInput formControlName="telefon" type="tel" inputmode="tel" autocomplete="off">
        </mat-form-field>
        <mat-form-field appearance="outline" subscriptSizing="dynamic">
          <mat-label>Napomena</mat-label>
          <textarea matInput formControlName="napomena" rows="2" placeholder="npr. dvorana, što ih zanima, kad se javljaju"></textarea>
        </mat-form-field>
        @if (greska()) {
          <p class="greska">{{ greska() }}</p>
        }
      </form>
    </mat-dialog-content>
    <mat-dialog-actions align="end">
      <button mat-button mat-dialog-close type="button">Odustani</button>
      <button mat-flat-button type="submit" form="brzi-upit" [disabled]="forma.invalid || sprema()">Spremi upit</button>
    </mat-dialog-actions>
  `,
  styles: `
    form { display: flex; flex-direction: column; gap: 12px; padding-top: 6px; }
    .ishod { display: flex; gap: 10px; align-items: flex-start; padding: 10px 12px; border-radius: 12px; font-size: 14px; }
    .slobodno { background: var(--wm-uspjeh-pozadina); color: var(--wm-uspjeh); }
    .zauzeto { background: var(--wm-opasno-pozadina); color: var(--wm-opasno); }
    .upit { background: var(--wm-upozorenje-pozadina); color: var(--wm-upozorenje); }
  `,
})
export class BrziUpitComponent {
  private api = inject(ApiService);
  private router = inject(Router);
  private dialogRef = inject(MatDialogRef<BrziUpitComponent>);

  readonly izIso = izIso;
  readonly par = par;
  readonly stanje = stanjeDatuma;
  readonly opis = OPIS_STANJA;
  dostupnost = signal<Dostupnost | null>(null);
  sprema = signal(false);
  greska = signal('');

  forma = inject(FormBuilder).group({
    datum: [null as Date | null, Validators.required],
    ime: ['', [Validators.required, Validators.maxLength(200)]],
    telefon: ['', Validators.maxLength(50)],
    napomena: ['', Validators.maxLength(2000)],
  });

  provjeri(datum: Date | null): void {
    this.dostupnost.set(null);
    if (!datum) {
      return;
    }
    // Zauzet datum ne brani spremanje: upit se smije zapisati i tada, samo se odmah vidi stanje
    this.api.dostupnost(uIso(datum)).subscribe({ next: (d) => this.dostupnost.set(d), error: () => undefined });
  }

  spremi(): void {
    if (this.forma.invalid || this.sprema()) {
      return;
    }
    const f = this.forma.getRawValue();
    this.sprema.set(true);
    this.greska.set('');
    this.api.brziUpit({ datum: uIso(f.datum!), ime: f.ime!.trim(), telefon: f.telefon || null, napomena: f.napomena || null })
      .subscribe({
        next: (v) => {
          this.dialogRef.close();
          this.router.navigate(['/vjencanja', v.id]);
        },
        error: (err) => {
          this.greska.set(porukaGreske(err));
          this.sprema.set(false);
        },
      });
  }
}
