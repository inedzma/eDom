package ba.unze.edom.server.service;

import ba.unze.edom.common.dto.*;
import ba.unze.edom.common.util.Opcine;
import ba.unze.edom.server.entity.*;
import ba.unze.edom.server.exception.PrijavaException;
import ba.unze.edom.server.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Prijava koju uposlenik unosi s papira.
 * Sve u jednoj transakciji: ako bilo šta ne valja, u bazi ne ostaje ništa.
 */
@Service
@RequiredArgsConstructor
public class RucniUnosService {

    private final StudentEvidencijaService studentEvidencija;
    private final RegistracijaService registracijaService;
    private final StudentRepository studentRepository;
    private final KorisnikRepository korisnikRepository;
    private final PrijavaRepository prijavaRepository;
    private final StatusPrijaveRepository statusRepository;
    private final VrstaKriterijaRepository vrstaKriterijaRepository;
    private final VrstaDokumentaRepository vrstaDokumentaRepository;
    private final SocijalniStatusRepository socijalniStatusRepository;
    private final BodovanjeService bodovanjeService;
    private final PasswordEncoder passwordEncoder;
    private final ApplicationEventPublisher dogadjaji;
    private final SifraStudenta sifraStudenta;

    // ---------- pretraga prije unosa ----------

    @Transactional(readOnly = true)
    public Optional<StudentPronadjenDTO> nadjiPoJmbg(String jmbg) {
        return studentRepository.findByJmbg(jmbg).map(s -> new StudentPronadjenDTO(
                s.getIdStudent(),
                new StudentUnosDTO(s.getIme(), s.getPrezime(), s.getJmbg(), s.getBrojIndeksa(),
                        s.getEmail(), s.getTelefon(), s.getFakultet(), s.getGodinaStudija(),
                        s.getAdresa(), s.getImeRoditelja(),
                        s.getSocijalniStatus() != null ? s.getSocijalniStatus().getIdStatus() : null,
                        s.getBrojLicneKarte(), s.getTelefonRoditelja(), s.getKanton(), s.getOpcina()),
                korisnikRepository.findByStudent_IdStudent(s.getIdStudent()).isPresent()));
    }

    // ---------- pregled bodova (korak 5), ništa se ne snima ----------

    @Transactional(readOnly = true)
    public RezultatBodovanja pregledBodova(RucniUnosDTO z) {

        provjeriStudenta(z.student());
        studentRepository.findByJmbg(z.student().jmbg())
                .ifPresent(s -> provjeriDuplikat(s.getIdStudent(), z.prijava().akademskaGodina()));

        Prijava p = napravi(z);

        Integer idStatusa = z.student().idSocijalnogStatusa();
        if (idStatusa != null) {
            p.setSocijalniStatus(socijalniStatusRepository.findById(idStatusa).orElse(null));
        }

        return bodovanjeService.izracunaj(p);
    }

    // ---------- unos ----------

    /** Lični podaci koje prijava traži, isto kao web korak 1. */
    private void provjeriStudenta(StudentUnosDTO s) {
        if (s.brojLicneKarte() == null || s.brojLicneKarte().isBlank()) {
            throw new PrijavaException("Broj lične karte je obavezan.");
        }
        if (s.telefonRoditelja() == null || s.telefonRoditelja().isBlank()) {
            throw new PrijavaException("Telefon roditelja je obavezan.");
        }
        if (!Opcine.postoji(s.kanton(), s.opcina())) {
            throw new PrijavaException("Odaberite kanton i općinu iz liste.");
        }
    }

    @Transactional
    public RucniUnosRezultatDTO unesi(RucniUnosDTO z) {

        provjeriStudenta(z.student());
        // 1. student, po istim pravilima kao web registracija
        Student student = studentEvidencija.nadjiIliKreiraj(z.student());
        sifraStudenta.dodijeli(student);

        // 2. nalog, ako ga nema; lozinku student postavlja sam preko linka iz maila
        boolean noviNalog = false;
        if (korisnikRepository.findByStudent_IdStudent(student.getIdStudent()).isEmpty()) {
            String nepoznataLozinka = passwordEncoder.encode(UUID.randomUUID().toString());
            registracijaService.napraviNalog(student, z.student().email(), nepoznataLozinka);
            noviNalog = true;
        }

        // 3. jedna prijava po akademskoj godini
        PrijavaUnosDTO pz = z.prijava();
        provjeriDuplikat(student.getIdStudent(), pz.akademskaGodina());

        Prijava p = napravi(z);
        p.setStudent(student);
        p.setSocijalniStatus(student.getSocijalniStatus());

        // 4. bodovanje = ujedno i validacija; nevalidni podaci poništavaju cijeli unos
        RezultatBodovanja r = bodovanjeService.izracunaj(p);
        p.setUkupniBodovi(r.potvrdjeno());

        prijavaRepository.save(p);

        dogadjaji.publishEvent(new PrijavaEvidentiranaEvent(
                z.student().email(), student.getIme(), pz.akademskaGodina(), noviNalog,
                SifraStudenta.format(student.getSifraBroj(), pz.akademskaGodina())));

        return new RucniUnosRezultatDTO(p.getIdPrijava(), student.getIdStudent(),
                noviNalog, r.potvrdjeno(), r.potencijalno());
    }

    // ---------- zajedničko za pregled i unos ----------

    private void provjeriDuplikat(Integer idStudenta, Integer akademskaGodina) {
        if (prijavaRepository.findByStudent_IdStudentAndAkademskaGodina(idStudenta, akademskaGodina).isPresent()) {
            throw new PrijavaException("Student već ima prijavu za akademsku " + akademskaGodina + ". godinu.");
        }
    }

    /** Prijava s kriterijima i dokumentima, bez studenta. Ne snima se ovdje. */
    private Prijava napravi(RucniUnosDTO z) {
        PrijavaUnosDTO pz = z.prijava();

        Integer upisPut = pz.upisGodinePut() != null ? pz.upisGodinePut() : 1;
        if (upisPut < 1) {
            throw new PrijavaException("Broj upisa godine mora biti najmanje 1.");
        }

        Prijava p = new Prijava();
        p.setAkademskaGodina(pz.akademskaGodina());
        p.setDatumPrijave(LocalDate.now());
        p.setStatus(statusRepository.findByNaziv(Statusi.NA_PREGLEDU)
                .orElseThrow(() -> new IllegalStateException("Status 'na pregledu' ne postoji.")));
        p.setGodinaStudija(z.student().godinaStudija());
        p.setUpisGodinePut(upisPut);
        p.setProsjek(pz.prosjek());
        p.setPolozeniIspiti(pz.polozeniIspiti());
        p.setBrojClanovaDomacinstva(pz.brojClanovaDomacinstva());
        p.setUkupnaPrimanja(pz.ukupnaPrimanja());
        p.setUdaljenostKm(pz.udaljenostKm());
        p.setNapomena(pz.napomena());

        // originali pred uposlenikom -> odmah verifikovano
        p.setUspjehVerifikovan(pz.uspjehVerifikovan());
        p.setUdaljenostVerifikovana(pz.udaljenostVerifikovana());
        p.setPrimanjaVerifikovana(pz.primanjaVerifikovana());

        // kriteriji, po šifri; bodove nosi baza, ne desktop
        for (KriterijUnosDTO kz : nullSafe(z.kriteriji())) {
            VrstaKriterija v = vrstaKriterijaRepository.findBySifra(kz.sifra())
                    .orElseThrow(() -> new PrijavaException("Nepoznat kriterij: " + kz.sifra()));

            if (v.isTraziParametar() && kz.parametar() == null) {
                throw new PrijavaException("Za kriterij '" + v.getNaziv() + "' morate unijeti "
                        + v.getNazivParametra().toLowerCase() + ".");
            }

            PrijavaKriterij k = new PrijavaKriterij();
            k.setVrsta(v);
            k.setParametar(v.isTraziParametar() ? kz.parametar() : null);
            k.setVerifikovan(kz.verifikovan());

            if (v.isTraziPeriod()) {
                if (kz.datumOd() == null || kz.datumDo() == null) {
                    throw new PrijavaException("Za kriterij '" + v.getNaziv() + "' unesite datum od i datum do.");
                }
                if (kz.datumOd().isAfter(kz.datumDo())) {
                    throw new PrijavaException("Za kriterij '" + v.getNaziv() + "' datum od ne može biti poslije datuma do.");
                }
                k.setDatumOd(kz.datumOd());
                k.setDatumDo(kz.datumDo());
            }

            p.dodajKriterij(k);
        }

        // dokumenti
        for (DokumentUnosDTO dz : nullSafe(z.dokumenti())) {
            VrstaDokumenta vrsta = vrstaDokumentaRepository.findById(dz.idVrste())
                    .orElseThrow(() -> new PrijavaException("Nepoznata vrsta dokumenta."));

            Dokument d = new Dokument();
            d.setVrstaDokumenta(vrsta);
            d.setNaziv(dz.naziv() != null && !dz.naziv().isBlank() ? dz.naziv().trim() : vrsta.getNaziv());
            d.setDatumUpload(LocalDate.now());
            d.setDostavljen(dz.dostavljen());
            d.setBrojBodova(0);
            d.setDokumentB64(dz.b64());
            p.dodajDokument(d);
        }

        return p;
    }

    private static <T> List<T> nullSafe(List<T> l) {
        return l != null ? l : List.of();
    }
}