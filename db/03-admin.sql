-- Probni administrator za prvo pokretanje.
-- Prijava: korisničko ime "admin", lozinka "Admin.2026!"  → PROMIJENITI odmah nakon prve prijave.
INSERT INTO korisnik (ime, prezime, username, password_hash, email, ulogaid_uloga, reset_pokusaji)
SELECT 'Administrator', 'Doma', 'admin',
       '$2a$10$rqdY2vXgxpAmTHSuXdXU1.NuYqzz9k4gomIchpumwmN5vY3bP/h1q',
       'admin@primjer.ba', id_uloga, 0
FROM uloga WHERE naziv = 'ADMIN';