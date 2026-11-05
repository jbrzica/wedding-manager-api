import { inject } from '@angular/core';
import { CanActivateFn, Router } from '@angular/router';
import { AuthService } from './auth.service';

export const authGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  return auth.prijavljen() ? true : inject(Router).createUrlTree(['/prijava']);
};

// Prijavljenog korisnika ne puštamo natrag na prijavu/registraciju
export const gostGuard: CanActivateFn = () => {
  const auth = inject(AuthService);
  return auth.prijavljen() ? inject(Router).createUrlTree(['/']) : true;
};
