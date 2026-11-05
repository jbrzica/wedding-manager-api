import { registerLocaleData } from '@angular/common';
import localeHr from '@angular/common/locales/hr';
import { bootstrapApplication } from '@angular/platform-browser';
import { appConfig } from './app/app.config';
import { App } from './app/app';

// Hrvatski nazivi mjeseci i dana, format "2.800,00 €"
registerLocaleData(localeHr);

bootstrapApplication(App, appConfig)
  .catch((err) => console.error(err));
