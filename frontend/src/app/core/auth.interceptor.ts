import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, throwError } from 'rxjs';
import { AuthService } from './auth.service';

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const auth = inject(AuthService);
  const token = auth.token();

  const zahtjev = token
    ? req.clone({ setHeaders: { Authorization: `Bearer ${token}` } })
    : req;

  return next(zahtjev).pipe(
    catchError((err: HttpErrorResponse) => {
      // Token je istekao ili je neispravan: vrati korisnika na prijavu
      if (err.status === 401 && token) {
        auth.odjava();
      }
      return throwError(() => err);
    }),
  );
};
