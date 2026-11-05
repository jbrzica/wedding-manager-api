import { Component } from '@angular/core';
import { MatTabsModule } from '@angular/material/tabs';
import { PaketiComponent } from './paketi.component';
import { DodaciComponent } from './dodaci.component';
import { SuradniciComponent } from './suradnici.component';
import { TvrtkaComponent } from './tvrtka.component';

@Component({
  selector: 'app-postavke',
  imports: [MatTabsModule, PaketiComponent, DodaciComponent, SuradniciComponent, TvrtkaComponent],
  template: `
    <h1>Postavke</h1>
    <mat-tab-group mat-stretch-tabs="false" animationDuration="0ms">
      <mat-tab label="Paketi"><app-paketi /></mat-tab>
      <mat-tab label="Dodaci"><app-dodaci /></mat-tab>
      <mat-tab label="Suradnici"><app-suradnici /></mat-tab>
      <mat-tab label="Tvrtka"><app-tvrtka /></mat-tab>
    </mat-tab-group>
  `,
})
export class PostavkeComponent {}
