import { ApplicationConfig, LOCALE_ID, provideBrowserGlobalErrorListeners } from '@angular/core';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { provideRouter, withComponentInputBinding } from '@angular/router';
import { DateAdapter, MAT_DATE_FORMATS, MAT_DATE_LOCALE, MAT_NATIVE_DATE_FORMATS } from '@angular/material/core';

import { routes } from './app.routes';
import { authInterceptor } from './core/auth.interceptor';
import { HrDateAdapter } from './core/hr-date-adapter';

export const appConfig: ApplicationConfig = {
  providers: [
    provideBrowserGlobalErrorListeners(),
    provideRouter(routes, withComponentInputBinding()),
    // Svaki HTTP zahtjev prolazi kroz interceptor koji dodaje token
    provideHttpClient(withInterceptors([authInterceptor])),
    { provide: DateAdapter, useClass: HrDateAdapter },
    { provide: MAT_DATE_FORMATS, useValue: MAT_NATIVE_DATE_FORMATS },
    { provide: LOCALE_ID, useValue: 'hr' },
    { provide: MAT_DATE_LOCALE, useValue: 'hr' },
  ],
};
