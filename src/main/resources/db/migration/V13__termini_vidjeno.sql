-- Do kada je pojedini korisnik pregledao nove online rezervacije (za obavijest u aplikaciji)
CREATE TABLE termini_vidjeno (
    korisnik_id BIGINT PRIMARY KEY REFERENCES korisnik (id) ON DELETE CASCADE,
    vidjeno_do  TIMESTAMP WITH TIME ZONE NOT NULL
);
