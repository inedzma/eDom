package ba.unze.edom.server.service;

import ba.unze.edom.common.dto.*;
import ba.unze.edom.server.entity.*;
import ba.unze.edom.server.exception.PrijavaException;
import ba.unze.edom.server.repository.*;
import lombok.RequiredArgsConstructor;
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
    private final org.springframework.context.ApplicationEventPublisher dogadjaji;

    // ---------- pretraga prije unosa ----------

    @Transactional(readOnly = true)
    public Optional<StudentPronadjenDTO> nadjiPoJmbg(String jmbg) {
        return studentRepository.findByJmbg(jmbg).map(s -> new StudentPronadjenDTO(
                s.getIdStudent(),
                new StudentUnosDTO(s.getIme(), s.getPrezime(), s.getJmbg(), s.getBrojIndeksa(),
                        s.getEmail(), s.getTelefon(), s.getFakultet(), s.getGodinaStudija(),
                        s.getAdresa(), s.getImeRoditelja(),
                        s.getSocijalniStatus() != null ? s.getSocijalniStatus().getIdStatus() : null),
                korisnikRepository.findByStudent_IdStudent(s.getIdStudent()).isPresent()));
    }

    // ---------- unos ----------

    @Transactional
    public RucniUnosRezultatDTO unesi(RucniUnosDTO z) {

        // 1. student, po istim pravilima kao web registracija
        Student student = studentEvidencija.nadjiIliKreiraj(z.student());

        // 2. nalog, ako ga nema; lozinku student postavlja sam preko linka iz maila
        boolean noviNalog = false;
        if (korisnikRepository.findByStudent_IdStudent(student.getIdStudent()).isEmpty()) {
            String nepoznataLozinka = passwordEncoder.encode(UUID.randomUUID().toString());
            registracijaService.napraviNalog(student, z.student().email(), nepoznataLozinka);
            noviNalog = true;
        }

        // 3. jedna prijava po akademskoj godini
        PrijavaUnosDTO pz = z.prijava();
        if (prijavaRepository.findByStudent_IdStudentAndAkademskaGodina(
                student.getIdStudent(), pz.akademskaGodina()).isPresent()) {
            throw new PrijavaException("Student već ima prijavu za akademsku "
                    + pz.akademskaGodina() + ". godinu.");
        }

        Prijava p = new Prijava();
        p.setStudent(student);
        p.setAkademskaGodina(pz.akademskaGodina());
        p.setDatumPrijave(LocalDate.now());
        p.setStatus(statusRepository.findByNaziv(Statusi.NA_PREGLEDU)
                .orElseThrow(() -> new IllegalStateException("Status 'na pregledu' ne postoji.")));
        p.setGodinaStudija(z.student().godinaStudija());
        p.setProsjek(pz.prosjek());
        p.setPolozeniIspiti(pz.polozeniIspiti());
        p.setBrojClanovaDomacinstva(pz.brojClanovaDomacinstva());
        p.setUkupnaPrimanja(pz.ukupnaPrimanja());
        p.setUdaljenostKm(pz.udaljenostKm());
        p.setNapomena(pz.napomena());
        p.setSocijalniStatus(student.getSocijalniStatus());

        // originali pred uposlenikom -> odmah verifikovano
        p.setUspjehVerifikovan(pz.uspjehVerifikovan());
        p.setUdaljenostVerifikovana(pz.udaljenostVerifikovana());
        p.setPrimanjaVerifikovana(pz.primanjaVerifikovana());

        // 4. kriteriji, po šifri; bodove nosi baza, ne desktop
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
            p.dodajKriterij(k);
        }

        // 5. dokumenti
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

        // 6. bodovanje = ujedno i validacija; nevalidni podaci poništavaju cijeli unos
        RezultatBodovanja r = bodovanjeService.izracunaj(p);
        p.setUkupniBodovi(r.potvrdjeno());

        prijavaRepository.save(p);

        dogadjaji.publishEvent(new PrijavaEvidentiranaEvent(
                z.student().email(), student.getIme(), pz.akademskaGodina(), noviNalog));

        return new RucniUnosRezultatDTO(p.getIdPrijava(), student.getIdStudent(),
                noviNalog, r.potvrdjeno(), r.potencijalno());
    }

    private static <T> List<T> nullSafe(List<T> l) {
        return l != null ? l : List.of();
    }
}