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
    path: 'poziv',
    canActivate: [gostGuard],
    loadComponent: () => import('./auth/poziv.component').then((m) => m.PozivComponent),
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
        path: 'postavke',
        loadComponent: () => import('./postavke/postavke.component').then((m) => m.PostavkeComponent),
      },
    ],
  },
  { path: '**', redirectTo: '' },
];
