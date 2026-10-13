package ba.unze.edom.server.service;

import ba.unze.edom.common.dto.KorisnikIzmjenaDTO;
import ba.unze.edom.common.dto.KorisnikProfilDTO;
import ba.unze.edom.common.dto.PromjenaLozinkeDTO;
import ba.unze.edom.server.entity.Korisnik;
import ba.unze.edom.server.exception.KorisnikException;
import ba.unze.edom.server.repository.KorisnikRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/** Operacije nad korisničkim nalogom, zajedničke za web i desktop. */
@Service
@RequiredArgsConstructor
public class KorisnikService {

    /** Najmanje 8 znakova, veliko i malo slovo, broj i specijalni znak. */
    private static final Pattern JAKA_LOZINKA =
            Pattern.compile("^(?=.*[A-Z])(?=.*[a-z])(?=.*\\d)(?=.*[^A-Za-z0-9]).{8,}$");

    private final KorisnikRepository korisnikRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional(readOnly = true)
    public KorisnikProfilDTO profil(String username) {
        return uDto(nadji(username));
    }

    @Transactional
    public KorisnikProfilDTO izmijeni(String username, KorisnikIzmjenaDTO dto) {
        Korisnik k = nadji(username);
        k.setIme(formatirajIme(dto.ime()));
        k.setPrezime(formatirajIme(dto.prezime()));
        return uDto(k);
    }

    @Transactional
    public void promijeniLozinku(String username, PromjenaLozinkeDTO dto) {
        Korisnik k = nadji(username);

        if (!passwordEncoder.matches(dto.staraLozinka(), k.getPasswordHash())) {
            throw new KorisnikException("Pogrešna trenutna lozinka.");
        }
        if (dto.staraLozinka().equals(dto.novaLozinka())) {
            throw new KorisnikException("Nova lozinka mora biti različita od stare.");
        }
        provjeriJacinu(dto.novaLozinka());

        k.setPasswordHash(passwordEncoder.encode(dto.novaLozinka()));
    }

    /** Javno, da je koriste i registracija i reset lozinke na webu. */
    public void provjeriJacinu(String lozinka) {
        if (lozinka == null || !JAKA_LOZINKA.matcher(lozinka).matches()) {
            throw new KorisnikException(
                    "Lozinka mora imati najmanje 8 znakova, veliko i malo slovo, broj i specijalni znak.");
        }
    }

    // ---------- pomoćne ----------

    private Korisnik nadji(String username) {
        return korisnikRepository.findByUsername(username)
                .orElseThrow(() -> new KorisnikException("Korisnik ne postoji."));
    }

    private KorisnikProfilDTO uDto(Korisnik k) {
        return new KorisnikProfilDTO(
                k.getIme(), k.getPrezime(), k.getUsername(), k.getEmail(),
                k.getUloga() != null ? k.getUloga().getNaziv() : null,
                k.getZadnjaPrijava());
    }
    /** "  aMRA   hadžić " -> "Amra Hadžić" */
    private String formatirajIme(String s) {
        return Arrays.stream(s.trim().split("\\s+"))
                .map(r -> r.substring(0, 1).toUpperCase() + r.substring(1).toLowerCase())
                .collect(Collectors.joining(" "));
    }
}