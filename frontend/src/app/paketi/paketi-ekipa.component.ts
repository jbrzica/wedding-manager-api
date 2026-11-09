import { Component, inject } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { MatTabsModule } from '@angular/material/tabs';
import { PaketiComponent } from './paketi.component';
import { DodaciComponent } from './dodaci.component';
import { SuradniciComponent } from './suradnici.component';

const TABOVI = ['paketi', 'dodaci', 'suradnici'];

@Component({
  selector: 'app-paketi-ekipa',
  imports: [MatTabsModule, PaketiComponent, DodaciComponent, SuradniciComponent],
  template: `
    <h1>Paketi i ekipa</h1>
    <mat-tab-group mat-stretch-tabs="false" animationDuration="0ms" [selectedIndex]="pocetniTab">
      <mat-tab label="Paketi"><app-paketi /></mat-tab>
      <mat-tab label="Dodaci"><app-dodaci /></mat-tab>
      <mat-tab label="Suradnici"><app-suradnici /></mat-tab>
    </mat-tab-group>
  `,
})
export class PaketiEkipaComponent {
  // ?tab=suradnici otvara odmah taj tab (npr. iz liste „Prvi koraci“)
  readonly pocetniTab = Math.max(0, TABOVI.indexOf(inject(ActivatedRoute).snapshot.queryParamMap.get('tab') ?? ''));
}
