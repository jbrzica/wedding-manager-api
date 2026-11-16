import { Component, inject, OnInit, signal } from '@angular/core';
import { CurrencyPipe, DatePipe } from '@angular/common';
import { ActivatedRoute } from '@angular/router';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { PotvrdaRezervacije, TerminiApi } from '../termini/termini-api';
import { izIso } from '../core/datumi';
import { porukaGreske } from '../core/greska';

// Link iz maila s potvrdom: klijent vidi svoj termin i može ga otkazati, bez prijave
@Component({
  selector: 'app-otkazivanje',
  imports: [CurrencyPipe, DatePipe, MatButtonModule, MatIconModule, MatProgressBarModule],
  template: `
    <div class="stranica">
      @if (ucitava()) {
        <mat-progress-bar mode="indeterminate" />
      } @else if (r(); as r) {
        <div class="firma">{{ r.tvrtka }}</div>
        <h1>{{ r.otkazano ? 'Termin je otkazan' : r.dogadjaj }}</h1>
        <div class="kartica" [class.otkazano]="r.otkazano">
          <strong>{{ izIso(r.datum) | date: 'EEEE, d. MMMM' }} u {{ r.vrijeme.slice(0, 5) }}</strong>
          <div>{{ r.nazivPaketa }}, {{ r.trajanjeMinuta }} min · {{ r.cijena | currency: 'EUR' : 'symbol' : '1.0-2' }}</div>
          <div class="sivo">{{ r.ime }}@if (r.lokacija) { · {{ r.lokacija }} }</div>
        </div>
        @if (r.otkazano) {
          <p>Vrijeme je ponovno slobodno za druge. Hvala što si javio/la.</p>
        } @else if (r.moguceOtkazati) {
          <p>Ne možeš doći? Otkaži termin, da ga može uzeti netko drugi. Online otkazivanje je moguće najkasnije {{ r.otkazSati }} sata prije termina.</p>
          <button mat-flat-button type="button" class="otkazi" [disabled]="salje()" (click)="otkazi()">Otkaži termin</button>
        } @else if (r.terminProsao) {
          <p class="sivo">Termin je prošao.</p>
        } @else {
          <p>Online otkazivanje je moguće najkasnije {{ r.otkazSati }} sata prije termina.
            @if (r.telefonTvrtke) { Ako ne možeš doći, nazovi studio. } @else { Ako ne možeš doći, javi se studiju. }</p>
        }
        @if (r.telefonTvrtke) { <p class="sivo">Za promjenu termina nazovi {{ r.tvrtka }}: <a [href]="'tel:' + r.telefonTvrtke">{{ r.telefonTvrtke }}</a></p> }
        @if (greska()) { <p class="greska">{{ greska() }}</p> }
      } @else {
        <h1>Rezervacija ne postoji</h1>
        <p class="sivo">Link je možda nepotpun. Otvori ga ponovno iz maila.</p>
      }
    </div>
  `,
  styles: `
    :host { display: block; min-height: 100vh; background: #fbfaf7; color: #1d1b18; }
    .stranica { max-width: 520px; margin: 0 auto; padding: 40px 20px; }
    .firma { font-size: 12px; letter-spacing: .12em; text-transform: uppercase; font-weight: 700; color: #4f5b4e; }
    h1 { font-family: Georgia, 'Times New Roman', serif; font-style: italic; font-weight: 400; font-size: 30px; margin: 6px 0 16px; }
    .kartica { background: #fff; border: 1px solid #e8e1d7; border-radius: 16px; padding: 16px; line-height: 1.6; }
    .kartica strong::first-letter { text-transform: uppercase; }
    .kartica.otkazano { opacity: .6; text-decoration: line-through; }
    .sivo { color: #6f675c; font-size: 14px; }
    .otkazi { --mat-button-filled-container-color: #a8232c; border-radius: 999px; }
    .greska { color: #b3392a; }
  `,
})
export class OtkazivanjeComponent implements OnInit {
  private api = inject(TerminiApi);
  private kod = inject(ActivatedRoute).snapshot.paramMap.get('kod') ?? '';

  readonly izIso = izIso;
  r = signal<PotvrdaRezervacije | null>(null);
  ucitava = signal(true);
  salje = signal(false);
  greska = signal('');

  ngOnInit(): void {
    this.api.rezervacija(this.kod).subscribe({
      next: (r) => { this.r.set(r); this.ucitava.set(false); },
      error: () => this.ucitava.set(false),
    });
  }

  otkazi(): void {
    this.salje.set(true);
    this.api.otkaziJavno(this.kod).subscribe({
      next: (r) => { this.r.set(r); this.salje.set(false); },
      error: (err) => { this.greska.set(porukaGreske(err)); this.salje.set(false); },
    });
  }
}
