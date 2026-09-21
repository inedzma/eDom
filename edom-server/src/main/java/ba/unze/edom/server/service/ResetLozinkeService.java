package ba.unze.edom.server.service;

import ba.unze.edom.server.entity.Korisnik;
import ba.unze.edom.server.exception.RegistracijaException;
import ba.unze.edom.server.repository.KorisnikRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
@RequiredArgsConstructor
public class ResetLozinkeService {

    static final int TRAJANJE_MINUTA = 15;
    static final int MAX_POKUSAJA = 5;
    static final int MIN_DUZINA_LOZINKE = 8;

    private static final SecureRandom RANDOM = new SecureRandom();

    private final KorisnikRepository korisnikRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    /**
     * Salje kod ako nalog postoji. Ako ne postoji, ne radi nista —
     * kontroler svejedno prikazuje istu poruku.
     */
    @Transactional
    public void zatraziKod(String email) {

        if (email == null || email.isBlank()) return;

        korisnikRepository.findByEmail(email.trim()).ifPresent(k -> {
            String kod = generisiKod();

            k.setResetTokenHash(passwordEncoder.encode(kod));
            k.setResetTokenIstek(Instant.now().plus(TRAJANJE_MINUTA, ChronoUnit.MINUTES));
            k.setResetPokusaji(0);

            emailService.posaljiResetKod(k.getEmail(), kod);
        });
    }

    @Transactional(noRollbackFor = RegistracijaException.class)
    public void promijeniLozinku(String email, String kod,
                                 String nova, String potvrda) {

        // ista poruka za nepostojeci nalog i pogresan kod
        final String NEISPRAVNO = "Kod nije ispravan ili je istekao.";

        Korisnik k = korisnikRepository.findByEmail(email == null ? "" : email.trim())
                .orElseThrow(() -> new RegistracijaException(NEISPRAVNO));

        if (k.getResetTokenHash() == null || k.getResetTokenIstek() == null) {
            throw new RegistracijaException(NEISPRAVNO);
        }

        if (k.getResetTokenIstek().isBefore(Instant.now())) {
            ponisti(k);
            throw new RegistracijaException("Kod je istekao. Zatražite novi.");
        }

        if (k.getResetPokusaji() >= MAX_POKUSAJA) {
            ponisti(k);
            throw new RegistracijaException(
                    "Previše neuspješnih pokušaja. Zatražite novi kod.");
        }

        if (kod == null || !passwordEncoder.matches(kod.trim(), k.getResetTokenHash())) {
            k.setResetPokusaji(k.getResetPokusaji() + 1);   // ostaje sacuvano
            throw new RegistracijaException(NEISPRAVNO);
        }

        if (nova == null || nova.length() < MIN_DUZINA_LOZINKE) {
            throw new RegistracijaException(
                    "Lozinka mora imati najmanje " + MIN_DUZINA_LOZINKE + " znakova.");
        }

        if (!nova.equals(potvrda)) {
            throw new RegistracijaException("Lozinke se ne podudaraju.");
        }

        k.setPasswordHash(passwordEncoder.encode(nova));
        ponisti(k);   // kod se moze iskoristiti samo jednom
    }

    private void ponisti(Korisnik k) {
        k.setResetTokenHash(null);
        k.setResetTokenIstek(null);
        k.setResetPokusaji(0);
    }

    private String generisiKod() {
        return String.valueOf(100000 + RANDOM.nextInt(900000));
    }
}