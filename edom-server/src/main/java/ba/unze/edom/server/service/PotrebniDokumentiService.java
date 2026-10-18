package ba.unze.edom.server.service;

import ba.unze.edom.server.dto.PrilozeniDokument;
import ba.unze.edom.server.dto.SpisakDokumenata;
import ba.unze.edom.server.dto.ZahtjevDokumenta;
import ba.unze.edom.server.entity.Prijava;
import ba.unze.edom.server.entity.PrijavaKriterij;
import ba.unze.edom.server.entity.VrstaDokumenta;
import ba.unze.edom.server.entity.VrstaKriterija;
import ba.unze.edom.server.exception.PrijavaException;
import ba.unze.edom.server.repository.DokumentRepository;
import ba.unze.edom.server.repository.PrijavaRepository;
import ba.unze.edom.server.repository.VrstaDokumentaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Pravi spisak dokumenata koje student treba priložiti, prema Konkursu 2026/2027 (tačka III).
 *
 * Obavezni dokumenti idu svima. Dodatni se pojavljuju samo za kriterije koje je
 * student označio, a koji dokument dokazuje koji kriterij piše u bazi
 * (vrsta_kriterija.dokaz_vrsta_id), ne u kodu.
 */
@Service
@RequiredArgsConstructor
public class PotrebniDokumentiService {

    // šifre iz tabele vrsta_dokumenta
    static final String SVJEDODZBA       = "SVJEDODZBA";
    static final String UVJERENJE_STUDIRANJE = "UVJERENJE_STUDIRANJE";
    static final String UVJERENJE_ISPITI = "UVJERENJE_ISPITI";
    static final String KOPIJA_INDEKSA   = "KOPIJA_INDEKSA";
    static final String KUCNA_LISTA      = "KUCNA_LISTA";
    static final String CIPS             = "CIPS";
    static final String UPLATNICA        = "UPLATNICA";
    static final String GARANCIJA        = "GARANCIJA";

    static final List<String> DOKUMENTI_CLANA = List.of(
            "CLAN_RADNI", "CLAN_PENZIJA", "CLAN_BIRO",
            "CLAN_NEZAPOSLEN", "CLAN_SKOLA", "CLAN_DIJETE", "CLAN_PRIMANJA");

    static final List<String> DOKUMENTI_GARANTA = List.of(
            "GARANT_RADNI", "GARANT_PENZIJA", "GARANT_FIRMA");

    private final PrijavaRepository prijavaRepository;
    private final VrstaDokumentaRepository vrstaDokumentaRepository;
    private final DokumentRepository dokumentRepository;

    @Transactional(readOnly = true)
    public SpisakDokumenata zaPrijavu(Integer idPrijave) {

        Prijava p = prijavaRepository.zaDetalje(idPrijave)
                .orElseThrow(() -> new PrijavaException("Prijava ne postoji."));

        Map<String, VrstaDokumenta> poSifri = vrstaDokumentaRepository.findBySifraIsNotNull().stream()
                .collect(Collectors.toMap(VrstaDokumenta::getSifra, v -> v));

        List<PrilozeniDokument> svi = dokumentRepository.prilozeniZaPrijavu(idPrijave);
        Set<Integer> rasporedjeni = new HashSet<>();

        // ---------------- obavezni ----------------
        List<ZahtjevDokumenta> obavezni = new ArrayList<>();

        // prijavni obrazac se ne prilaže: sistem ga pravi iz prijave i student ga štampa

        boolean brucos = Objects.equals(p.getGodinaStudija(), GodineStudija.PRVA);
        if (brucos) {
            obavezni.add(zahtjev("uspjeh", "Ovjerena kopija svjedočanstva iz IV razreda srednje škole (NE diplomu srednje škole, već svjedodžbu sa prosjekom)", null, true,
                    vrste(poSifri, SVJEDODZBA), 1, false, svi, rasporedjeni));
        } else {
            obavezni.add(zahtjev("uspjeh", "Dokaz o studiranju i položenim ispitima",
                    "Uvjerenje o redovnom studiranju i uvjerenje o položenim ispitima, ili ovjerena kopija indeksa s ocjenama. "
                            + "Ako imate više dokumenata, priložite svaki posebno.", true,
                    vrste(poSifri, UVJERENJE_STUDIRANJE, UVJERENJE_ISPITI, KOPIJA_INDEKSA), 1, false, svi, rasporedjeni));
        }

        obavezni.add(zahtjev("kucna", "Uvjerenje o zajedničkom domaćinstvu (kućna lista)", "Dokument izvaditi i ovjeriti u nadležnoj općini.", true,
                vrste(poSifri, KUCNA_LISTA), 1, false, svi, rasporedjeni));

        int ostalihClanova = p.getBrojClanovaDomacinstva() != null ? p.getBrojClanovaDomacinstva() - 1 : 0;
        if (ostalihClanova > 0) {
            obavezni.add(zahtjev("clanovi", "Dokumenti o primanjima članova domaćinstva",
                    "Za svakog člana osim vas. Jednom članu možete priložiti više dokumenata (npr. tri platne liste). Detaljne upute za svakog člana pogledajte [ovdje](/upute#dokumenti) ", true,
                    vrste(poSifri, DOKUMENTI_CLANA.toArray(String[]::new)),
                    ostalihClanova, true, svi, rasporedjeni));
        }

        obavezni.add(zahtjev("cips", "Potvrda o mjestu stalnog boravka (CIPS)", null, true,
                vrste(poSifri, CIPS), 1, false, svi, rasporedjeni));

        obavezni.add(zahtjev("uplatnica", "Uplatnica na 10,00 KM za troškove obrade podataka",
                "Podatke za uplatu i primjer popunjene uplatnice pogledajte [ovdje](/upute#uplatnica).", true,
                vrste(poSifri, UPLATNICA), 1, false, svi, rasporedjeni));

        obavezni.add(zahtjev("garancija", "Ovjerena garancija o preuzimanju obaveza",
                "[Preuzmite obrazac garancije](/dokumenti/garancija.pdf), popunite ga i ovjerite u općini ili kod notara.", true,
                vrste(poSifri, GARANCIJA), 1, false, svi, rasporedjeni));
        obavezni.add(zahtjev("garant", "Dokumenti garanta", null, true,
                vrste(poSifri, DOKUMENTI_GARANTA.toArray(String[]::new)), 1, false, svi, rasporedjeni));

        // ---------------- dodatni: samo za označene kriterije ----------------
        // više kriterija može imati isti dokaz (npr. RVI i invalidnost roditelja) -> jedan red
        // grupisanje po ID-u, ne po objektu: Hibernate zna vratiti proxy
        Map<Integer, VrstaDokumenta> dokazi = new LinkedHashMap<>();
        Map<Integer, List<String>> kriterijiPoDokazu = new LinkedHashMap<>();
        List<String> bezDokaza = new ArrayList<>();

        for (PrijavaKriterij k : p.getKriteriji()) {
            VrstaKriterija v = k.getVrsta();
            if (v == null) continue;
            VrstaDokumenta dokaz = v.getDokaz();
            if (dokaz != null) {
                dokazi.putIfAbsent(dokaz.getIdVrsta(), dokaz);
                kriterijiPoDokazu.computeIfAbsent(dokaz.getIdVrsta(), id -> new ArrayList<>()).add(v.getNaziv());
            } else {
                bezDokaza.add(v.getNaziv());
            }
        }

        List<ZahtjevDokumenta> dodatni = new ArrayList<>();
        dokazi.forEach((id, dokaz) -> dodatni.add(zahtjev(
                "dokaz-" + id,
                dokaz.getNaziv(),
                "Za kriterij: " + String.join(", ", kriterijiPoDokazu.get(id)),
                false, List.of(dokaz), 1, false, svi, rasporedjeni)));

        // ---------------- sve što nije raspoređeno ----------------
        List<PrilozeniDokument> ostali = svi.stream()
                .filter(d -> !rasporedjeni.contains(d.idDokument()))
                .toList();

        return new SpisakDokumenata(obavezni, dodatni, bezDokaza, ostali);
    }

    // ---------------- pomoćne ----------------

    /** Vrste po šiframa; šifra koja ne postoji u bazi se preskače. */
    private List<VrstaDokumenta> vrste(Map<String, VrstaDokumenta> poSifri, String... sifre) {
        return Arrays.stream(sifre)
                .map(poSifri::get)
                .filter(Objects::nonNull)
                .toList();
    }

    /** Zahtjev s već priloženim dokumentima odgovarajućih vrsta. */
    private ZahtjevDokumenta zahtjev(String kljuc, String naziv, String opis, boolean obavezan,
                                     List<VrstaDokumenta> vrste, int potrebno, boolean poClanu,
                                     List<PrilozeniDokument> svi, Set<Integer> rasporedjeni) {
        Set<Integer> idVrsta = vrste.stream().map(VrstaDokumenta::getIdVrsta).collect(Collectors.toSet());

        List<PrilozeniDokument> prilozeni = svi.stream()
                .filter(d -> d.idVrste() != null && idVrsta.contains(d.idVrste()))
                .filter(d -> !rasporedjeni.contains(d.idDokument()))
                .toList();

        prilozeni.forEach(d -> rasporedjeni.add(d.idDokument()));

        return new ZahtjevDokumenta(kljuc, naziv, opis, obavezan, vrste, potrebno, poClanu, prilozeni);
    }
}