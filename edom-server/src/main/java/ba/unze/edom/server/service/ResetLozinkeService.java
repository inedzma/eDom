package ba.unze.edom.server.service;

import ba.unze.edom.server.entity.Korisnik;
import ba.unze.edom.server.exception.RegistracijaException;
import ba.unze.edom.server.repository.KorisnikRepository;
import ba.unze.edom.server.validation.LozinkaPravila;
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

        if (email == null || email.isBlank()) {
            throw new RegistracijaException("Unesite e-mail adresu.");
        }

        Korisnik k = korisnikRepository.findByEmail(email.trim())
                .orElseThrow(() -> new RegistracijaException(
                        "Ne postoji nalog sa tom e-mail adresom."));

        String kod = generisiKod();

        k.setResetTokenHash(passwordEncoder.encode(kod));
        k.setResetTokenIstek(Instant.now().plus(TRAJANJE_MINUTA, ChronoUnit.MINUTES));
        k.setResetPokusaji(0);

        emailService.posaljiResetKod(k.getEmail(), kod);
    }

    @Transactional(noRollbackFor = RegistracijaException.class)
    public void promijeniLozinku(String email, String kod,
                                 String nova, String potvrda) {

        System.out.println(">>> SERVIS ulaz, email=" + email);

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
            k.setResetPokusaji(k.getResetPokusaji() + 1);
            throw new RegistracijaException(NEISPRAVNO);
        }

        String greskaLozinke = LozinkaPravila.poruka(nova);
        if (greskaLozinke != null) {
            throw new RegistracijaException(greskaLozinke);
        }

        if (!nova.equals(potvrda)) {
            throw new RegistracijaException("Lozinke se ne podudaraju.");
        }

        k.setPasswordHash(passwordEncoder.encode(nova));

        korisnikRepository.save(k);          // dodaj i ovo

        System.out.println(">>> UPISANO id=" + k.getIdKorisnik()
                + " novi hash=" + k.getPasswordHash());

        ponisti(k);

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