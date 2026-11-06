-- Tajni token za pretplatu na kalendar vjenčanja (Google ili Apple kalendar).
ALTER TABLE tvrtka ADD COLUMN kalendar_token VARCHAR(64) UNIQUE;
