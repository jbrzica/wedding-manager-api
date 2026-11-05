import { Component, computed, DestroyRef, inject, input, OnInit, signal } from '@angular/core';
import { CurrencyPipe } from '@angular/common';
import { FormArray, FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { debounceTime, forkJoin, merge } from 'rxjs';
import { MatCardModule } from '@angular/material/card';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';
import { MatProgressBarModule } from '@angular/material/progress-bar';
import { ApiService } from '../core/api.service';
import {
  Dodatak, IzvorUpita, Paket, Suradnik, UlogaSuradnika, VjencanjeDetalji, VjencanjeZahtjev,
} from '../core/modeli';
import { IZVORI_UPITA, ULOGE_SURADNIKA, kljucevi } from '../core/oznake';
import { porukaGreske } from '../core/greska';

@Component({
  selector: 'app-forma',
  imports: [
    ReactiveFormsModule, RouterLink, CurrencyPipe, MatCardModule, MatButtonModule, MatIconModule,
    MatFormFieldModule, MatInputModule, MatSelectModule, MatProgressBarModule,
  ],
  templateUrl: './forma.component.html',
  styleUrl: './forma.component.scss',
})
export class FormaComponent implements OnInit {
  private api = inject(ApiService);
  private router = inject(Router);
  private fb = inject(FormBuilder);
  private destroyRef = inject(DestroyRef);

  // Iz adrese: /vjencanja/:id/uredi ili /vjencanja/novo?datum=2026-06-14
  id = input<string>();
  datum = input<string>();

  urediMo = computed(() => !!this.id());
  paketi = signal<Paket[]>([]);
  dodaci = signal<Dodatak[]>([]);
  suradnici = signal<Suradnik[]>([]);
  upozorenja = signal<string[]>([]);
  ucitava = signal(true);
  sprema = signal(false);
  greska = signal('');

  // Dok korisnik sam ne promijeni cijenu, računamo je iz paketa i dodataka
  private cijenaRucno = false;

  readonly uloge = kljucevi(ULOGE_SURADNIKA);
  readonly oznakeUloga = ULOGE_SURADNIKA;
  readonly izvori = kljucevi(IZVORI_UPITA);
  readonly oznakeIzvora = IZVORI_UPITA;

  forma = this.fb.group({
    datum: ['', Validators.required],
    lokacija: [''],
    mlada: this.osobaGrupa(),
    mladozenja: this.osobaGrupa(),
    paketId: [null as number | null],
    dodatakIds: [[] as number[]],
    dogovorenaCijena: [null as number | null, Validators.min(0)],
    rokIsporuke: [''],
    izvorUpita: [null as IzvorUpita | null],
    napomene: [''],
    raspored: this.fb.array<FormGroup>([]),
    ekipa: this.fb.array<FormGroup>([]),
  });

  get raspored(): FormArray<FormGroup> {
    return this.forma.controls.raspored;
  }

  get ekipa(): FormArray<FormGroup> {
    return this.forma.controls.ekipa;
  }

  ngOnInit(): void {
    const sifrarnici = forkJoin({ paketi: this.api.paketi(), dodaci: this.api.dodaci(), suradnici: this.api.suradnici() });

    sifrarnici.subscribe({
      next: ({ paketi, dodaci, suradnici }) => {
        this.paketi.set(paketi);
        this.dodaci.set(dodaci);
        this.suradnici.set(suradnici);
        if (this.urediMo()) {
          this.api.detalji(Number(this.id())).subscribe({
            next: (v) => this.popuni(v),
            error: (err) => this.prikaziGresku(err),
          });
        } else {
          this.forma.controls.datum.setValue(this.datum() ?? '');
          this.dodajStavku('12:00', 'Dolazak kod mladenke');
          this.dodajStavku('', 'Crkva');
          this.dodajStavku('', 'Sala');
          this.ucitava.set(false);
        }
      },
      error: (err) => this.prikaziGresku(err),
    });

    merge(this.forma.controls.paketId.valueChanges, this.forma.controls.dodatakIds.valueChanges)
      .pipe(takeUntilDestroyed(this.destroyRef))
      .subscribe(() => this.izracunajCijenu());

    // Provjera preklapanja kad se promijeni datum ili ekipa (samo upozorenje, spremanje je dozvoljeno)
    merge(this.forma.controls.datum.valueChanges, this.ekipa.valueChanges)
      .pipe(debounceTime(400), takeUntilDestroyed(this.destroyRef))
      .subscribe(() => this.provjeriPreklapanja());
  }

  osobaGrupa(): FormGroup {
    return this.fb.group({
      ime: ['', Validators.required],
      prezime: ['', Validators.required],
      telefon: [''],
      email: ['', Validators.email],
      adresa: [''],
    });
  }

  dodajStavku(vrijeme = '', naziv = '', adresa = '', napomena = ''): void {
    this.raspored.push(this.fb.group({
      vrijeme: [vrijeme],
      naziv: [naziv, Validators.required],
      adresa: [adresa],
      napomena: [napomena],
    }));
  }

  dodajClana(suradnikId: number | null = null, uloga: UlogaSuradnika | null = null, honorar: number | null = null): void {
    this.ekipa.push(this.fb.group({
      suradnikId: [suradnikId, Validators.required],
      uloga: [uloga],
      honorar: [honorar, Validators.min(0)],
    }));
  }

  // Kad se odabere suradnik, predloži njegovu zadanu ulogu
  odabranSuradnik(index: number, suradnikId: number): void {
    const s = this.suradnici().find((x) => x.id === suradnikId);
    if (s) {
      this.ekipa.at(index).patchValue({ uloga: s.uloga });
    }
  }

  ukloni(niz: FormArray, index: number): void {
    niz.removeAt(index);
  }

  pomakni(index: number, za: number): void {
    const novi = index + za;
    if (novi < 0 || novi >= this.raspored.length) {
      return;
    }
    const stavka = this.raspored.at(index);
    this.raspored.removeAt(index);
    this.raspored.insert(novi, stavka);
  }

  cijenaRucnoPromijenjena(): void {
    this.cijenaRucno = true;
  }

  vratiIzracunatuCijenu(): void {
    this.cijenaRucno = false;
    this.izracunajCijenu();
  }

  izracunataCijena(): number {
    const paket = this.paketi().find((p) => p.id === this.forma.controls.paketId.value);
    const odabrani = this.forma.controls.dodatakIds.value ?? [];
    const dodaci = this.dodaci().filter((d) => odabrani.includes(d.id));
    return (paket?.cijena ?? 0) + dodaci.reduce((zbroj, d) => zbroj + d.cijena, 0);
  }

  spremi(): void {
    if (this.forma.invalid) {
      this.forma.markAllAsTouched();
      this.greska.set('Popunite obavezna polja (označena crveno).');
      return;
    }
    this.sprema.set(true);
    this.greska.set('');
    const zahtjev = this.zahtjev();
    const poziv = this.urediMo()
      ? this.api.izmijeniVjencanje(Number(this.id()), zahtjev)
      : this.api.stvoriVjencanje(zahtjev);
    poziv.subscribe({
      next: (v) => this.router.navigate(['/vjencanja', v.id]),
      error: (err) => {
        this.sprema.set(false);
        this.greska.set(porukaGreske(err));
      },
    });
  }

  private izracunajCijenu(): void {
    if (!this.cijenaRucno) {
      this.forma.controls.dogovorenaCijena.setValue(this.izracunataCijena());
    }
  }

  private provjeriPreklapanja(): void {
    const datum = this.forma.controls.datum.value;
    if (!datum) {
      this.upozorenja.set([]);
      return;
    }
    const ids = this.ekipa.getRawValue().map((c) => c['suradnikId']).filter((id): id is number => !!id);
    const izuzmi = this.id() ? Number(this.id()) : undefined;
    this.api.upozorenja(datum, ids, izuzmi).subscribe({
      next: (o) => this.upozorenja.set(o.upozorenja),
      error: () => this.upozorenja.set([]),
    });
  }

  private popuni(v: VjencanjeDetalji): void {
    // Postojeća cijena je dogovorena, ne smije se sama promijeniti kad se otvori forma
    this.cijenaRucno = true;
    this.forma.patchValue({
      datum: v.datum,
      lokacija: v.lokacija ?? '',
      mlada: { ...v.mlada },
      mladozenja: { ...v.mladozenja },
      paketId: v.paket?.id ?? null,
      dodatakIds: v.dodaci.map((d) => d.id),
      dogovorenaCijena: v.financije.dogovorenaCijena,
      rokIsporuke: v.rokIsporuke ?? '',
      izvorUpita: v.izvorUpita ?? null,
      napomene: v.napomene ?? '',
    });
    v.raspored.forEach((s) => this.dodajStavku(s.vrijeme?.slice(0, 5) ?? '', s.naziv, s.adresa ?? '', s.napomena ?? ''));
    v.ekipa.forEach((c) => this.dodajClana(c.suradnikId, c.uloga, c.honorar ?? null));
    this.ucitava.set(false);
  }

  private zahtjev(): VjencanjeZahtjev {
    const f = this.forma.getRawValue();
    const prazno = (s: string | null | undefined) => (s && s.trim() ? s.trim() : null);
    const osoba = (o: Record<string, string>) => ({
      ime: o['ime'].trim(),
      prezime: o['prezime'].trim(),
      telefon: prazno(o['telefon']),
      email: prazno(o['email']),
      adresa: prazno(o['adresa']),
    });
    return {
      datum: f.datum!,
      lokacija: prazno(f.lokacija),
      mlada: osoba(f.mlada),
      mladozenja: osoba(f.mladozenja),
      paketId: f.paketId,
      dodatakIds: f.dodatakIds ?? [],
      dogovorenaCijena: f.dogovorenaCijena,
      rokIsporuke: prazno(f.rokIsporuke),
      izvorUpita: f.izvorUpita,
      napomene: prazno(f.napomene),
      raspored: f.raspored.map((s) => ({
        vrijeme: prazno(s['vrijeme']),
        naziv: s['naziv'].trim(),
        adresa: prazno(s['adresa']),
        napomena: prazno(s['napomena']),
      })),
      ekipa: f.ekipa.map((c) => ({ suradnikId: c['suradnikId'], uloga: c['uloga'], honorar: c['honorar'] })),
    };
  }

  private prikaziGresku(err: unknown): void {
    this.greska.set(porukaGreske(err));
    this.ucitava.set(false);
  }
}
