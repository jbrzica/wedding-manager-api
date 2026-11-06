import { Component } from '@angular/core';
import { MatTabsModule } from '@angular/material/tabs';
import { PaketiComponent } from './paketi.component';
import { DodaciComponent } from './dodaci.component';
import { SuradniciComponent } from './suradnici.component';

@Component({
  selector: 'app-paketi-ekipa',
  imports: [MatTabsModule, PaketiComponent, DodaciComponent, SuradniciComponent],
  template: `
    <h1>Paketi i ekipa</h1>
    <mat-tab-group mat-stretch-tabs="false" animationDuration="0ms">
      <mat-tab label="Paketi"><app-paketi /></mat-tab>
      <mat-tab label="Dodaci"><app-dodaci /></mat-tab>
      <mat-tab label="Suradnici"><app-suradnici /></mat-tab>
    </mat-tab-group>
  `,
})
export class PaketiEkipaComponent {}
