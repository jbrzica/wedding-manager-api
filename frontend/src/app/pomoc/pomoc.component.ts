import { AfterViewInit, Component, ElementRef, inject, signal } from '@angular/core';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { MatExpansionModule } from '@angular/material/expansion';
import { MatIconModule } from '@angular/material/icon';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';

interface Tema {
  id: string;
  ikona: string;
  naslov: string;
  // Kratki odlomci; prazan red među njima u prikazu
  tekst: string[];
}

const TEME: Tema[] = [
  {
    id: 'pocetak', ikona: 'flag', naslov: 'Kako početi',
    tekst: [
      'Prvo upiši svoje pakete i dodatke (Paketi i ekipa). Kad poslije na vjenčanju odabereš paket, cijena se upiše sama.',
      'Zatim dodaj suradnike: snimatelje, druge fotografe, asistente. Njih stavljaš u ekipu pojedinog vjenčanja.',
      'Onda upiši vjenčanja. Dovoljni su datum i imena mladenaca, sve ostalo možeš dopuniti kasnije.',
      'Na kraju stavi vjenčanja u kalendar na mobitelu (Postavke) i dodaj aplikaciju na početni zaslon.',
    ],
  },
  {
    id: 'statusi', ikona: 'label', naslov: 'Vjenčanja i statusi',
    tekst: [
      'Upit: par se raspitao, termin još nije dogovoren. Potvrđeno: dogovoreno, obično nakon kapare. Odrađeno: vjenčanje je bilo, radi se obrada. Isporučeno: fotografije i video su predani. Otkazano: ne broji se u statistici ni u zauzetim datumima.',
      'Kad upišeš kaparu na vjenčanje koje je upit, status se sam promijeni u Potvrđeno. Ostale statuse mijenjaš sam u detaljima vjenčanja.',
      'Ako na isti datum već imaš vjenčanje ili je netko iz ekipe zauzet, forma te na to upozori prije spremanja.',
    ],
  },
  {
    id: 'financije', ikona: 'payments', naslov: 'Uplate, kapara i zarada',
    tekst: [
      'U detaljima vjenčanja upisuješ svaku uplatu: kaparu, rate i ostatak. Aplikacija sama računa koliko je još preostalo.',
      'Zarada = dogovorena cijena − honorari ekipe − ostali troškovi (npr. album, put, smještaj).',
      'Cijena se na vjenčanju pamti onakva kakva je bila kad je dogovorena. Ako kasnije promijeniš cijenu paketa, stara vjenčanja ostaju na staroj cijeni.',
    ],
  },
  {
    id: 'ekipa', ikona: 'groups', naslov: 'Ekipa i link suradnika',
    tekst: [
      'Na vjenčanju dodaješ ekipu i honorar svakome. Honorari ulaze u zaradu i u statistiku honorara.',
      'Svaki suradnik ima svoj link (Paketi i ekipa → Suradnici → ikona lanca). Na njemu bez prijave vidi samo vjenčanja na kojima radi: datum, raspored s adresama i kontakte. Cijene, uplate i honorari se ne vide.',
      'Ako suradnik više ne radi s vama, odaberi „Napravi novi link“ i stari odmah prestaje raditi.',
      'Suradnike ne pozivaj kao članove tvrtke: članovi vide sve, i cijene.',
    ],
  },
  {
    id: 'raspored', ikona: 'schedule', naslov: 'Raspored dana i dijeljenje',
    tekst: [
      'Na vjenčanju upiši raspored: vrijeme, što se događa i adresu (npr. 11:00 priprema kod mladenke, 15:00 crkva, 18:30 sala).',
      'Svoj tipični dan spremi gumbom „Spremi kao predložak“ ispod rasporeda. Svako novo vjenčanje onda kreće od njega, a na postojećem ga možeš uzeti gumbom „Uzmi predložak“.',
      'Gumbom za dijeljenje raspored se na mobitelu šalje WhatsAppom ili Viberom, a na računalu se kopira. Šalje se bez cijena.',
      'Kontrolna lista na vjenčanju (ugovor poslan, potpisan, odabir fotografija…) pomaže da se ništa ne zaboravi. Stavke možeš dodavati i brisati.',
    ],
  },
  {
    id: 'kalendar', ikona: 'event', naslov: 'Kalendar na mobitelu',
    tekst: [
      'U Postavkama je link za kalendar. Kopiraj ga i dodaj u kalendar.',
      'Google kalendar: na računalu otvori calendar.google.com → kod „Ostali kalendari“ klikni + → Iz URL-a → zalijepi link. Kalendar se onda pojavi i na mobitelu.',
      'iPhone: Postavke → Kalendar → Računi → Dodaj račun → Ostalo → Dodaj pretplatnički kalendar.',
      'Nova vjenčanja pojave se sama. Google kalendar zna kasniti i nekoliko sati, to je normalno.',
    ],
  },
  {
    id: 'pocetni-zaslon', ikona: 'install_mobile', naslov: 'Aplikacija na početnom zaslonu',
    tekst: [
      'Android (Chrome): otvori aplikaciju u Chromeu → tri točke gore desno → Dodaj na početni zaslon (ili Instaliraj aplikaciju).',
      'iPhone (mora biti Safari): gumb za dijeljenje (kvadrat sa strelicom) → Dodaj na početni zaslon.',
      'Aplikacija se tada otvara preko cijelog ekrana, kao prava aplikacija.',
    ],
  },
  {
    id: 'trazenje', ikona: 'tune', naslov: 'Pretraga, filteri i provjera datuma',
    tekst: [
      'Na početnoj stranici pretražuješ po imenu, prezimenu ili lokaciji.',
      'Filteri: razdoblje, više statusa odjednom, godina, „Nije plaćeno do kraja“ i „Kasni isporuka“. Filteri se pamte dok ih ne očistiš.',
      'Provjera datuma: kad te netko pita jesi li slobodan, upiši datum i odmah vidiš je li dan slobodan, tko iz ekipe je zauzet i ima li vjenčanja dan prije ili poslije.',
    ],
  },
  {
    id: 'statistika', ikona: 'insights', naslov: 'Statistika',
    tekst: [
      'Prihod je zbroj dogovorenih cijena potvrđenih vjenčanja u godini; upiti i otkazana se ne broje.',
      'Upiti → posao: koliko upisanih termina je postalo stvarni posao. Po izvorima vidiš isplati li se Instagram, Google ili preporuke.',
      'Još za naplatiti: koliko od dogovorenog posla te godine još nije uplaćeno.',
      'Slobodne subote pokazuju koliko termina u sezoni još možeš prodati.',
    ],
  },
  {
    id: 'kos', ikona: 'delete', naslov: 'Brisanje i koš',
    tekst: [
      'Obrisano vjenčanje ide u koš (izbornik računa → Koš), sa svim uplatama, rasporedom i ekipom. Odande ga možeš vratiti 30 dana.',
      'Nakon 30 dana briše se zauvijek. Ako želiš odmah, u košu odaberi „Obriši zauvijek“.',
    ],
  },
  {
    id: 'tvrtka', ikona: 'settings', naslov: 'Tvrtka, članovi i lozinka',
    tekst: [
      'U Postavkama mijenjaš naziv tvrtke, pozivaš članove (npr. supružnika ili partnera u poslu) i mijenjaš lozinku.',
      'Član tvrtke vidi i mijenja sve, uključujući cijene i zaradu.',
      'Zaboravljena lozinka: na stranici za prijavu odaberi „Zaboravljena lozinka“ i dobit ćeš šifru mailom.',
      'Podaci se svaku noć spremaju u sigurnosnu kopiju.',
    ],
  },
];

const PITANJA: { pitanje: string; odgovor: string }[] = [
  { pitanje: 'Promijenio sam cijenu paketa, a stara vjenčanja imaju staru cijenu?',
    odgovor: 'Tako je namjerno: na vjenčanju se pamti cijena koju ste dogovorili. Ako treba, promijeni cijenu na samom vjenčanju.' },
  { pitanje: 'Slučajno sam obrisao vjenčanje.',
    odgovor: 'Otvori izbornik računa → Koš i klikni Vrati. Vjenčanje se vraća sa svim podacima.' },
  { pitanje: 'Suradniku link ne radi.',
    odgovor: 'Vjerojatno je napravljen novi link. U Suradnicima mu pošalji link ponovno.' },
  { pitanje: 'Novo vjenčanje se ne vidi u Google kalendaru.',
    odgovor: 'Google osvježava pretplaćene kalendare svakih nekoliko sati. Pričekaj; iPhone je obično brži.' },
  { pitanje: 'Zašto je vjenčanje samo od sebe postalo Potvrđeno?',
    odgovor: 'Upisana je kapara. Kapara znači da je termin dogovoren, pa aplikacija sama mijenja status iz Upita u Potvrđeno.' },
];

@Component({
  selector: 'app-pomoc',
  imports: [RouterLink, MatExpansionModule, MatIconModule, MatFormFieldModule, MatInputModule],
  template: `
    <h1>Pomoć</h1>
    <p class="sivo uvod">Kratka objašnjenja kako radi aplikacija. Klikni temu da je otvoriš.</p>

    <mat-form-field appearance="outline" class="trazi" subscriptSizing="dynamic">
      <mat-icon matPrefix>search</mat-icon>
      <input matInput placeholder="Traži, npr. kapara, kalendar, link…" (input)="upit.set($any($event.target).value)">
    </mat-form-field>

    <mat-accordion multi>
      @for (t of vidljiveTeme(); track t.id) {
        <mat-expansion-panel [expanded]="t.id === otvorena || !!upit()" [id]="'tema-' + t.id">
          <mat-expansion-panel-header>
            <mat-panel-title><mat-icon>{{ t.ikona }}</mat-icon> {{ t.naslov }}</mat-panel-title>
          </mat-expansion-panel-header>
          @for (o of t.tekst; track $index) { <p>{{ o }}</p> }
        </mat-expansion-panel>
      }
    </mat-accordion>

    <h2>Česta pitanja</h2>
    <mat-accordion>
      @for (p of vidljivaPitanja(); track p.pitanje) {
        <mat-expansion-panel [expanded]="!!upit()">
          <mat-expansion-panel-header><mat-panel-title>{{ p.pitanje }}</mat-panel-title></mat-expansion-panel-header>
          <p>{{ p.odgovor }}</p>
        </mat-expansion-panel>
      }
    </mat-accordion>

    @if (!vidljiveTeme().length && !vidljivaPitanja().length) {
      <p class="sivo">Ništa nije pronađeno za „{{ upit() }}“.</p>
    }

    <p class="sivo kraj">Povratak na <a routerLink="/">vjenčanja</a>.</p>
  `,
  styles: `
    :host { display: block; max-width: 760px; }
    .uvod { margin: 0 0 16px; }
    .trazi { width: 100%; margin-bottom: 16px; }
    mat-accordion { display: block; border: 1px solid var(--mat-sys-outline-variant); border-radius: 12px; overflow: hidden; }
    .mat-expansion-panel { box-shadow: none !important; border-radius: 0 !important; }
    .mat-expansion-panel + .mat-expansion-panel { border-top: 1px solid var(--mat-sys-outline-variant); }
    /* Dulja pitanja idu u dva reda, pa zaglavlje ne smije imati fiksnu visinu */
    .mat-expansion-panel-header { height: auto !important; min-height: 52px; padding-top: 6px; padding-bottom: 6px; }
    mat-panel-title { display: flex; align-items: center; gap: 10px; font-weight: 600; }
    mat-panel-title mat-icon { color: var(--mat-sys-primary); }
    p { line-height: 1.6; margin: 0 0 10px; }
    h2 { margin: 32px 0 12px; font-size: 18px; }
    .kraj { margin-top: 24px; }
  `,
})
export class PomocComponent implements AfterViewInit {
  private el = inject(ElementRef<HTMLElement>);
  readonly otvorena = inject(ActivatedRoute).snapshot.queryParamMap.get('tema');
  upit = signal('');

  vidljiveTeme(): Tema[] {
    const u = this.upit().trim().toLowerCase();
    return u ? TEME.filter((t) => (t.naslov + ' ' + t.tekst.join(' ')).toLowerCase().includes(u)) : TEME;
  }

  vidljivaPitanja() {
    const u = this.upit().trim().toLowerCase();
    return u ? PITANJA.filter((p) => (p.pitanje + ' ' + p.odgovor).toLowerCase().includes(u)) : PITANJA;
  }

  // ?tema=kalendar otvara i pokazuje tu temu (npr. iz liste „Prvi koraci“)
  ngAfterViewInit(): void {
    if (this.otvorena) {
      setTimeout(() => this.el.nativeElement.querySelector('#tema-' + this.otvorena)?.scrollIntoView({ block: 'start' }));
    }
  }
}
