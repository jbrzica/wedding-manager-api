import { Routes } from '@angular/router';
import { authGuard, gostGuard } from './core/auth.guard';
import { ShellComponent } from './layout/shell.component';

export const routes: Routes = [
  // Stranice za neprijavljene
  {
    path: 'prijava',
    canActivate: [gostGuard],
    loadComponent: () => import('./auth/prijava.component').then((m) => m.PrijavaComponent),
  },
  {
    path: 'registracija',
    canActivate: [gostGuard],
    loadComponent: () => import('./auth/registracija.component').then((m) => m.RegistracijaComponent),
  },
  {
    path: 'potvrda',
    loadComponent: () => import('./auth/potvrda.component').then((m) => m.PotvrdaComponent),
  },
  {
    path: 'zaboravljena-lozinka',
    canActivate: [gostGuard],
    loadComponent: () => import('./auth/zaboravljena-lozinka.component').then((m) => m.ZaboravljenaLozinkaComponent),
  },
  {
    path: 'poziv',
    canActivate: [gostGuard],
    loadComponent: () => import('./auth/poziv.component').then((m) => m.PozivComponent),
  },

  // Osobni link suradnika: bez prijave, pristup daje tajni token
  {
    path: 's/:token',
    loadComponent: () => import('./javno/suradnik-pregled.component').then((m) => m.SuradnikPregledComponent),
  },

  // Aplikacija: samo za prijavljene
  {
    path: '',
    component: ShellComponent,
    canActivate: [authGuard],
    children: [
      {
        path: '',
        loadComponent: () => import('./vjencanja/popis.component').then((m) => m.PopisComponent),
      },
      {
        path: 'kalendar',
        loadComponent: () => import('./vjencanja/kalendar.component').then((m) => m.KalendarComponent),
      },
      {
        path: 'vjencanja/novo',
        loadComponent: () => import('./vjencanja/forma.component').then((m) => m.FormaComponent),
      },
      {
        path: 'vjencanja/:id/uredi',
        loadComponent: () => import('./vjencanja/forma.component').then((m) => m.FormaComponent),
      },
      {
        path: 'vjencanja/:id',
        loadComponent: () => import('./vjencanja/detalji.component').then((m) => m.DetaljiComponent),
      },
      {
        path: 'statistika',
        loadComponent: () => import('./statistika/statistika.component').then((m) => m.StatistikaComponent),
      },
      {
        path: 'paketi',
        loadComponent: () => import('./paketi/paketi-ekipa.component').then((m) => m.PaketiEkipaComponent),
      },
      {
        path: 'postavke',
        loadComponent: () => import('./postavke/postavke.component').then((m) => m.PostavkeComponent),
      },
    ],
  },
  { path: '**', redirectTo: '' },
];
