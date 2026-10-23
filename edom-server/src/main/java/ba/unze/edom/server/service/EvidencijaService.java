package ba.unze.edom.server.service;

import ba.unze.edom.common.dto.*;
import ba.unze.edom.server.entity.Prijava;
import ba.unze.edom.server.entity.Smjestaj;
import ba.unze.edom.server.entity.Student;
import ba.unze.edom.server.exception.PrijavaException;
import ba.unze.edom.server.repository.PrijavaRepository;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/** Centralna evidencija: sve prijave godine s podacima o studentu i smještaju. */
@Service
@RequiredArgsConstructor
public class EvidencijaService {

    private static final ZoneId ZONA = ZoneId.of("Europe/Sarajevo");

    @PersistenceContext
    private EntityManager em;

    private final PrijavaRepository prijavaRepository;
    private final RangListaService rangListaService;

    @Transactional(readOnly = true)
    public EvidencijaDTO lista(Integer godina) {
        Integer g = godina != null ? godina : prijavaRepository.zadnjaAkademskaGodina();
        if (g == null) return new EvidencijaDTO(null, List.of());

        List<Prijava> prijave = em.createQuery(
                        "SELECT p FROM Prijava p JOIN FETCH p.student s JOIN FETCH p.status st "
                                + "WHERE p.akademskaGodina = :g AND st.naziv <> :uIzradi "
                                + "ORDER BY s.prezime, s.ime", Prijava.class)
                .setParameter("g", g)
                .setParameter("uIzradi", Statusi.U_IZRADI)
                .getResultList();

        Map<Integer, Smjestaj> smjestaji = smjestajPoPrijavi(g);
        Set<Integer> uvjerenjeUzPrijavu = uvjerenjeUzPrijavu(g);

        RangListaAdminDTO rang = rangListaService.zaAdmina(g, null, false);
        Set<Integer> primljeni = Stream.of(rang.brucosi(), rang.saUslovom())
                .flatMap(List::stream)
                .filter(RangRedDTO::primljen)
                .map(RangRedDTO::idPrijava)
                .collect(Collectors.toSet());

        List<EvidencijaRedDTO> redovi = prijave.stream()
                .map(p -> red(p, smjestaji.get(p.getIdPrijava()),
                        uvjerenjeUzPrijavu.contains(p.getIdPrijava()),
                        primljeni.contains(p.getIdPrijava())))
                .toList();

        return new EvidencijaDTO(g, redovi);
    }

    /** Kvačica → datum (današnji ako je nova, postojeći ako je već bila); bez kvačice → null. */
    @Transactional
    public void sacuvajOznake(Integer idPrijave, EvidencijaOznakeDTO z) {
        List<Smjestaj> svi = em.createQuery(
                        "SELECT s FROM Smjestaj s WHERE s.prijava.idPrijava = :id ORDER BY s.idSmjestaj DESC",
                        Smjestaj.class)
                .setParameter("id", idPrijave)
                .getResultList();
        Smjestaj s = svi.stream()
                .filter(x -> !Smjestaj.ISELJEN.equals(x.getStatus()))
                .findFirst()
                .orElse(svi.isEmpty() ? null : svi.get(0));

        if (s == null) {
            throw new PrijavaException("Student nema dodijeljenu sobu — oznake za useljenje upisuju se nakon dodjele sobe.");
        }

        LocalDate danas = LocalDate.now(ZONA);
        s.setKarticaPreuzeta(datum(s.getKarticaPreuzeta(), z.karticaPreuzeta(), danas));
        s.setKarticaRazduzena(z.karticaPreuzeta()
                ? datum(s.getKarticaRazduzena(), z.karticaRazduzena(), danas) : null);
        s.setLjekarskoPredano(datum(s.getLjekarskoPredano(), z.ljekarskoPredano(), danas));
        s.setUvjerenjeFakultetPredano(datum(s.getUvjerenjeFakultetPredano(), z.uvjerenjeFakultetPredano(), danas));
    }

    // ---------- pomoćne ----------

    private EvidencijaRedDTO red(Prijava p, Smjestaj sm, boolean uvjerenjeUzPrijavu, boolean primljen) {
        Student s = p.getStudent();
        String status = p.getStatus() != null ? p.getStatus().getNaziv() : null;

        return new EvidencijaRedDTO(
                p.getIdPrijava(), s.getIdStudent(),
                SifraStudenta.format(s.getSifraBroj(), p.getAkademskaGodina()),
                s.getIme(), s.getPrezime(), s.getImeRoditelja(), s.getJmbg(), s.getBrojLicneKarte(),
                s.getBrojIndeksa(), s.getEmail(), s.getTelefon(), s.getTelefonRoditelja(), s.getSpol(),
                s.getKanton(), s.getOpcina(), s.getAdresa(),
                s.getFakultet(), UslovUpisa.godinaStudija(p), p.getAkademskaGodina(), p.getDatumPrijave(),
                status, kategorija(p, status), primljen, p.getUkupniBodovi(),
                sm != null ? sm.getSoba().getBroj() : null,
                sm != null ? sm.getStatus() : null,
                sm != null ? sm.getDatumUseljenja() : null,
                sm != null ? sm.getDatumIseljenja() : null,
                sm != null ? sm.getKljuceviPreuzeti() : 0,
                sm != null ? sm.getKljuceviVraceni() : 0,
                sm != null ? sm.getKarticaPreuzeta() : null,
                sm != null ? sm.getKarticaRazduzena() : null,
                sm != null ? sm.getLjekarskoPredano() : null,
                sm != null ? sm.getUvjerenjeFakultetPredano() : null,
                uvjerenjeUzPrijavu,
                s.getNapomena());
    }

    private String kategorija(Prijava p, String status) {
        boolean naListi = Statusi.ODOBRENO.equals(status) || Statusi.ZAKLJUCENO.equals(status)
                || Statusi.NA_CEKANJU.equals(status);
        if (!naListi) return "—";
        if (UslovUpisa.jeBrucos(p)) return "Brucoš";
        return Statusi.NA_CEKANJU.equals(status) ? "Lista čekanja" : "S uslovom";
    }

    /** Po prijavi: zadnji smještaj koji nije ISELJEN; ako su svi iseljeni, zadnji. */
    private Map<Integer, Smjestaj> smjestajPoPrijavi(Integer g) {
        List<Smjestaj> svi = em.createQuery(
                        "SELECT s FROM Smjestaj s JOIN FETCH s.soba "
                                + "WHERE s.prijava.akademskaGodina = :g ORDER BY s.idSmjestaj", Smjestaj.class)
                .setParameter("g", g)
                .getResultList();

        Map<Integer, Smjestaj> m = new HashMap<>();
        for (Smjestaj s : svi) {
            Integer id = s.getPrijava().getIdPrijava();
            Smjestaj postojeci = m.get(id);
            if (postojeci == null
                    || Smjestaj.ISELJEN.equals(postojeci.getStatus())
                    || !Smjestaj.ISELJEN.equals(s.getStatus())) {
                m.put(id, s);
            }
        }
        return m;
    }

    /**
     * Prijave koje uz sebe imaju uvjerenje s fakulteta (po nazivu vrste dokumenta).
     * Ako se kod tebe vrsta zove drugačije, prilagodi LIKE uslove.
     */
    private Set<Integer> uvjerenjeUzPrijavu(Integer g) {
        return new HashSet<>(em.createQuery(
                        "SELECT DISTINCT d.prijava.idPrijava FROM Dokument d "
                                + "WHERE d.prijava.akademskaGodina = :g "
                                + "AND LOWER(d.vrstaDokumenta.naziv) LIKE '%uvjerenje%' "
                                + "AND (LOWER(d.vrstaDokumenta.naziv) LIKE '%studir%' "
                                + "  OR LOWER(d.vrstaDokumenta.naziv) LIKE '%fakultet%' "
                                + "  OR LOWER(d.vrstaDokumenta.naziv) LIKE '%ispit%')", Integer.class)
                .setParameter("g", g)
                .getResultList());
    }

    private static LocalDate datum(LocalDate stari, boolean oznaceno, LocalDate danas) {
        if (!oznaceno) return null;
        return stari != null ? stari : danas;
    }
}