import { inicijali, par, zaKoliko } from './imena';

describe('imena', () => {
  it('par spaja oba imena znakom &', () => {
    expect(par('Ana Horvat', 'Ivo Kovač')).toBe('Ana Horvat & Ivo Kovač');
  });

  // Brzi upit zna samo tko se javio; mladoženja je prazan
  it('par bez mladoženje nema znak &', () => {
    expect(par('Ana Horvat', '')).toBe('Ana Horvat');
    expect(par('Ana Horvat', null)).toBe('Ana Horvat');
    expect(par(undefined, undefined)).toBe('');
  });

  it('inicijali uzimaju prva slova prve dvije riječi', () => {
    expect(inicijali('ana marija horvat')).toBe('AM');
    expect(inicijali('Josip')).toBe('J');
    expect(inicijali('  ')).toBe('');
  });

  it('zaKoliko opisuje dane riječima', () => {
    expect(zaKoliko(0)).toBe('danas');
    expect(zaKoliko(1)).toBe('sutra');
    expect(zaKoliko(-1)).toBe('jučer');
    expect(zaKoliko(9)).toBe('za 9 dana');
    expect(zaKoliko(21)).toBe('za 21 dan');
    expect(zaKoliko(11)).toBe('za 11 dana');
    expect(zaKoliko(-3)).toBe('prije 3 dana');
  });
});
