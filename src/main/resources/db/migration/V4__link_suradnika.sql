-- Tajni token za osobni link suradnika (njegova vjenčanja bez cijena).
ALTER TABLE suradnik ADD COLUMN pristupni_token VARCHAR(64) UNIQUE;
