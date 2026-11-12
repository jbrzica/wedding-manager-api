import { stanjeDatuma } from './dostupnost';
import { Dostupnost, StatusVjencanja, VjencanjeSazetak } from './modeli';

function dan(...statusi: StatusVjencanja[]): Dostupnost {
  const vjencanja = statusi.map((status, id) => ({ id, status }) as VjencanjeSazetak);
  return { datum: '2027-06-12', slobodno: vjencanja.length === 0, vjencanja, blizu: [], suradnici: [] };
}

describe('stanjeDatuma', () => {
  it('dan bez vjenčanja je slobodan', () => {
    expect(stanjeDatuma(dan())).toBe('slobodno');
  });

  // Zbog upita se drugi par ne smije odbiti: datum još nije prodan
  it('dan na kojem su samo upiti nije zauzet', () => {
    expect(stanjeDatuma(dan('UPIT', 'UPIT'))).toBe('upit');
  });

  it('jedno potvrđeno vjenčanje čini dan zauzetim, i uz upit', () => {
    expect(stanjeDatuma(dan('UPIT', 'POTVRDENO'))).toBe('zauzeto');
    expect(stanjeDatuma(dan('ODRADENO'))).toBe('zauzeto');
  });
});
