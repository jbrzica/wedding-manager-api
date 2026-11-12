import { HttpErrorResponse, HttpInterceptorFn, HttpRequest } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, switchMap, throwError } from 'rxjs';
import { AuthService } from './auth.service';

// Prijava, osvježavanje i javne stranice ne nose access token i ne pokreću osvježavanje
function bezTokena(req: HttpRequest<unknown>): boolean {
  return req.url.startsWith('/api/auth/') && !req.url.startsWith('/api/auth/ja')
    && !req.url.startsWith('/api/auth/promjena-lozinke')
    || req.url.startsWith('/api/javno/');
}

function sTokenom(req: HttpRequest<unknown>, token: string | null): HttpRequest<unknown> {
  return token ? req.clone({ setHeaders: { Authorization: `Bearer ${token}` } }) : req;
}

/**
 * Svakom zahtjevu dodaje access token. Kad token istekne (401), jednom tiho osvježi
 * prijavu preko kolačića i ponovi zahtjev; tek ako ni to ne uspije, vraća na prijavu.
 */
export const authInterceptor: HttpInterceptorFn = (req, next) => {
  if (bezTokena(req)) {
    return next(req);
  }
  const auth = inject(AuthService);

  return next(sTokenom(req, auth.token())).pipe(
    catchError((err: HttpErrorResponse) => {
      if (err.status !== 401) {
        return throwError(() => err);
      }
      return auth.osvjezi().pipe(
        switchMap((o) => {
          if (!o) {
            // Bez mreže prijava ostaje (vide se spremljeni podaci); na prijavu vodi samo stvarno istekla
            if (!auth.prijavljen()) {
              auth.istekla();
            }
            return throwError(() => err);
          }
          return next(sTokenom(req, o.token));
        }),
      );
    }),
  );
};
