package ba.unze.edom.server.service;

import ba.unze.edom.common.dto.DokumentDTO;
import ba.unze.edom.common.dto.DokumentiSpisakDTO;
import ba.unze.edom.common.dto.SpisakUnosDTO;
import ba.unze.edom.common.dto.SpisakUnosZahtjevDTO;
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
import ba.unze.edom.server.repository.VrstaKriterijaRepository;
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
 *
 * Isti spisak koriste web (zaPrijavu) i desktop (zaUnos), pa su pravila na jednom mjestu.
 */
@Service
@RequiredArgsConstructor
public class PotrebniDokumentiService {

    // šifre iz tabele vrsta_dokumenta
    static final String SVJEDODZBA           = "SVJEDODZBA";
    static final String UVJERENJE_STUDIRANJE = "UVJERENJE_STUDIRANJE";
    static final String UVJERENJE_ISPITI     = "UVJERENJE_ISPITI";
    static final String KOPIJA_INDEKSA       = "KOPIJA_INDEKSA";
    static final String KUCNA_LISTA          = "KUCNA_LISTA";
    static final String CIPS                 = "CIPS";
    static final String UPLATNICA            = "UPLATNICA";
    static final String GARANCIJA            = "GARANCIJA";

    static final List<String> DOKUMENTI_CLANA = List.of(
            "CLAN_RADNI", "CLAN_PENZIJA", "CLAN_BIRO",
            "CLAN_NEZAPOSLEN", "CLAN_SKOLA", "CLAN_DIJETE", "CLAN_PRIMANJA");

    static final List<String> DOKUMENTI_GARANTA = List.of(
            "GARANT_RADNI", "GARANT_PENZIJA", "GARANT_FIRMA");

    private final PrijavaRepository prijavaRepository;
    private final VrstaDokumentaRepository vrstaDokumentaRepository;
    private final VrstaKriterijaRepository vrstaKriterijaRepository;
    private final DokumentRepository dokumentRepository;

    /** Jedan red spiska, prije nego što se zna šta je priloženo. */
    private record Sablon(String kljuc, String naziv, String opis, boolean obavezan,
                          List<VrstaDokumenta> vrste, int potrebno, boolean poClanu,
                          List<String> sifreKriterija) {}

    private record Pravila(List<Sablon> obavezni, List<Sablon> dodatni, List<VrstaKriterija> bezDokaza) {}

    // =====================================================================
    //  WEB: spisak za postojeću prijavu, s već priloženim dokumentima
    // =====================================================================

    @Transactional(readOnly = true)
    public SpisakDokumenata zaPrijavu(Integer idPrijave) {

        Prijava p = prijavaRepository.zaDetalje(idPrijave)
                .orElseThrow(() -> new PrijavaException("Prijava ne postoji."));

        List<VrstaKriterija> kriteriji = p.getKriteriji().stream()
                .map(PrijavaKriterij::getVrsta)
                .filter(Objects::nonNull)
                .toList();

        Pravila pr = pravila(p.getGodinaStudija(), p.getBrojClanovaDomacinstva(), kriteriji);

        List<PrilozeniDokument> svi = dokumentRepository.prilozeniZaPrijavu(idPrijave);
        Set<Integer> rasporedjeni = new HashSet<>();

        List<ZahtjevDokumenta> obavezni = new ArrayList<>();
        for (Sablon s : pr.obavezni()) obavezni.add(uZahtjev(s, svi, rasporedjeni));

        List<ZahtjevDokumenta> dodatni = new ArrayList<>();
        for (Sablon s : pr.dodatni()) dodatni.add(uZahtjev(s, svi, rasporedjeni));

        List<String> bezDokaza = pr.bezDokaza().stream().map(VrstaKriterija::getNaziv).toList();

        List<PrilozeniDokument> ostali = svi.stream()
                .filter(d -> !rasporedjeni.contains(d.idDokument()))
                .toList();

        return new SpisakDokumenata(obavezni, dodatni, bezDokaza, ostali);
    }

    // =====================================================================
    //  DESKTOP: spisak za prijavu koja se tek unosi (ništa još nije u bazi)
    // =====================================================================

    @Transactional(readOnly = true)
    public SpisakUnosDTO zaUnos(SpisakUnosZahtjevDTO z) {

        List<VrstaKriterija> kriteriji = (z.sifreKriterija() != null ? z.sifreKriterija() : List.<String>of())
                .stream()
                .distinct()
                .map(vrstaKriterijaRepository::findBySifra)
                .flatMap(Optional::stream)
                .toList();

        Pravila pr = pravila(z.godinaStudija(), z.brojClanova(), kriteriji);

        return new SpisakUnosDTO(
                pr.obavezni().stream().map(this::uDto).toList(),
                pr.dodatni().stream().map(this::uDto).toList(),
                pr.bezDokaza().stream()
                        .map(v -> new SpisakUnosDTO.Kriterij(v.getSifra(), v.getNaziv()))
                        .toList());
    }

    // =====================================================================
    //  DESKTOP: spisak za postojeću prijavu (admin označava dostavljene)
    // =====================================================================

    @Transactional(readOnly = true)
    public DokumentiSpisakDTO zaAdmina(Integer idPrijave) {

        Prijava p = prijavaRepository.zaDetalje(idPrijave)
                .orElseThrow(() -> new PrijavaException("Prijava ne postoji."));

        List<VrstaKriterija> kriteriji = p.getKriteriji().stream()
                .map(PrijavaKriterij::getVrsta)
                .filter(Objects::nonNull)
                .toList();

        Pravila pr = pravila(p.getGodinaStudija(), p.getBrojClanovaDomacinstva(), kriteriji);

        Map<Integer, DokumentDTO> poId = new LinkedHashMap<>();
        for (DokumentDTO d : dokumentRepository.pregledZaAdmina(idPrijave)) poId.putIfAbsent(d.idDokument(), d);

        List<PrilozeniDokument> svi = dokumentRepository.prilozeniZaPrijavu(idPrijave);
        Set<Integer> rasporedjeni = new HashSet<>();

        List<DokumentiSpisakDTO.Zahtjev> obavezni = new ArrayList<>();
        for (Sablon s : pr.obavezni()) obavezni.add(uAdminRed(s, svi, poId, rasporedjeni));

        List<DokumentiSpisakDTO.Zahtjev> dodatni = new ArrayList<>();
        for (Sablon s : pr.dodatni()) dodatni.add(uAdminRed(s, svi, poId, rasporedjeni));

        List<DokumentDTO> ostali = poId.values().stream()
                .filter(d -> !rasporedjeni.contains(d.idDokument()))
                .toList();

        return new DokumentiSpisakDTO(obavezni, dodatni,
                pr.bezDokaza().stream().map(VrstaKriterija::getNaziv).toList(), ostali);
    }

    private DokumentiSpisakDTO.Zahtjev uAdminRed(Sablon s, List<PrilozeniDokument> svi,
                                                 Map<Integer, DokumentDTO> poId, Set<Integer> rasporedjeni) {
        Set<Integer> idVrsta = s.vrste().stream().map(VrstaDokumenta::getIdVrsta).collect(Collectors.toSet());

        List<DokumentDTO> prilozeni = svi.stream()
                .filter(d -> d.idVrste() != null && idVrsta.contains(d.idVrste()))
                .filter(d -> !rasporedjeni.contains(d.idDokument()))
                .map(d -> poId.get(d.idDokument()))
                .filter(Objects::nonNull)
                .toList();
        prilozeni.forEach(d -> rasporedjeni.add(d.idDokument()));

        return new DokumentiSpisakDTO.Zahtjev(s.kljuc(), s.naziv(), bezLinkova(s.opis()), s.obavezan(),
                s.potrebno(), s.poClanu(),
                s.vrste().stream().map(v -> new DokumentiSpisakDTO.Vrsta(v.getIdVrsta(), v.getNaziv())).toList(),
                prilozeni);
    }

    // =====================================================================
    //  PRAVILA: koji dokumenti trebaju (zajedničko za web i desktop)
    // =====================================================================

    private Pravila pravila(Integer godinaStudija, Integer brojClanova, List<VrstaKriterija> kriteriji) {

        Map<String, VrstaDokumenta> poSifri = vrstaDokumentaRepository.findBySifraIsNotNull().stream()
                .collect(Collectors.toMap(VrstaDokumenta::getSifra, v -> v));

        // ---------------- obavezni ----------------
        List<Sablon> obavezni = new ArrayList<>();

        // prijavni obrazac se ne prilaže: sistem ga pravi iz prijave i student ga štampa

        boolean brucos = Objects.equals(godinaStudija, GodineStudija.PRVA);
        if (brucos) {
            obavezni.add(obavezan("uspjeh",
                    "Ovjerena kopija svjedočanstva iz IV razreda srednje škole (NE diplomu srednje škole, već svjedodžbu sa prosjekom)",
                    null,
                    vrste(poSifri, SVJEDODZBA)));
        } else {
            obavezni.add(obavezan("uspjeh", "Dokaz o studiranju i položenim ispitima",
                    "Uvjerenje o redovnom studiranju i uvjerenje o položenim ispitima, ili ovjerena kopija indeksa s ocjenama. "
                            + "Ako imate više dokumenata, priložite svaki posebno.",
                    vrste(poSifri, UVJERENJE_STUDIRANJE, UVJERENJE_ISPITI, KOPIJA_INDEKSA)));
        }

        obavezni.add(obavezan("kucna", "Uvjerenje o zajedničkom domaćinstvu (kućna lista)",
                "Dokument izvaditi i ovjeriti u nadležnoj općini.",
                vrste(poSifri, KUCNA_LISTA)));

        int ostalihClanova = brojClanova != null ? brojClanova - 1 : 0;
        if (ostalihClanova > 0) {
            obavezni.add(new Sablon("clanovi", "Dokumenti o primanjima članova domaćinstva",
                    "Za svakog člana osim vas. Jednom članu možete priložiti više dokumenata (npr. tri platne liste). "
                            + "Detaljne upute za svakog člana pogledajte [ovdje](/upute#dokumenti)",
                    true, vrste(poSifri, DOKUMENTI_CLANA.toArray(String[]::new)),
                    ostalihClanova, true, List.of()));
        }

        obavezni.add(obavezan("cips", "Potvrda o mjestu stalnog boravka (CIPS)", null,
                vrste(poSifri, CIPS)));

        obavezni.add(obavezan("uplatnica", "Uplatnica na 10,00 KM za troškove obrade podataka",
                "Podatke za uplatu i primjer popunjene uplatnice pogledajte [ovdje](/upute#uplatnica).",
                vrste(poSifri, UPLATNICA)));

        obavezni.add(obavezan("garancija", "Ovjerena garancija o preuzimanju obaveza",
                "Preuzmite obrazac garancije: [Word (za popunjavanje na računaru)](/dokumenti/garancija.doc) "
                        + "ili [PDF (za štampanje)](/dokumenti/garancija.pdf). "
                        + "Popunite ga i ovjerite u općini ili kod notara.",
                vrste(poSifri, GARANCIJA)));

        obavezni.add(obavezan("garant", "Dokumenti garanta", null,
                vrste(poSifri, DOKUMENTI_GARANTA.toArray(String[]::new))));

        // ---------------- dodatni: samo za označene kriterije ----------------
        // više kriterija može imati isti dokaz (npr. RVI i invalidnost roditelja) -> jedan red
        // grupisanje po ID-u, ne po objektu: Hibernate zna vratiti proxy
        Map<Integer, VrstaDokumenta> dokazi = new LinkedHashMap<>();
        Map<Integer, List<VrstaKriterija>> kriterijiPoDokazu = new LinkedHashMap<>();
        List<VrstaKriterija> bezDokaza = new ArrayList<>();

        for (VrstaKriterija v : kriteriji) {
            VrstaDokumenta dokaz = v.getDokaz();
            if (dokaz != null) {
                dokazi.putIfAbsent(dokaz.getIdVrsta(), dokaz);
                kriterijiPoDokazu.computeIfAbsent(dokaz.getIdVrsta(), id -> new ArrayList<>()).add(v);
            } else {
                bezDokaza.add(v);
            }
        }

        List<Sablon> dodatni = new ArrayList<>();
        dokazi.forEach((id, dokaz) -> {
            List<VrstaKriterija> zaDokaz = kriterijiPoDokazu.get(id);
            dodatni.add(new Sablon(
                    "dokaz-" + id,
                    dokaz.getNaziv(),
                    "Za kriterij: " + zaDokaz.stream().map(VrstaKriterija::getNaziv).collect(Collectors.joining(", ")),
                    false, List.of(dokaz), 1, false,
                    zaDokaz.stream().map(VrstaKriterija::getSifra).toList()));
        });

        return new Pravila(obavezni, dodatni, bezDokaza);
    }

    // ---------------- pomoćne ----------------

    private static Sablon obavezan(String kljuc, String naziv, String opis, List<VrstaDokumenta> vrste) {
        return new Sablon(kljuc, naziv, opis, true, vrste, 1, false, List.of());
    }

    /** Vrste po šiframa; šifra koja ne postoji u bazi se preskače. */
    private List<VrstaDokumenta> vrste(Map<String, VrstaDokumenta> poSifri, String... sifre) {
        return Arrays.stream(sifre)
                .map(poSifri::get)
                .filter(Objects::nonNull)
                .toList();
    }

    /** Web: šablon s već priloženim dokumentima odgovarajućih vrsta. */
    private ZahtjevDokumenta uZahtjev(Sablon s, List<PrilozeniDokument> svi, Set<Integer> rasporedjeni) {
        Set<Integer> idVrsta = s.vrste().stream().map(VrstaDokumenta::getIdVrsta).collect(Collectors.toSet());

        List<PrilozeniDokument> prilozeni = svi.stream()
                .filter(d -> d.idVrste() != null && idVrsta.contains(d.idVrste()))
                .filter(d -> !rasporedjeni.contains(d.idDokument()))
                .toList();

        prilozeni.forEach(d -> rasporedjeni.add(d.idDokument()));

        return new ZahtjevDokumenta(s.kljuc(), s.naziv(), s.opis(), s.obavezan(),
                s.vrste(), s.potrebno(), s.poClanu(), prilozeni);
    }

    /** Desktop: isti šablon, bez linkova u opisu (na desktopu se ne mogu kliknuti). */
    private SpisakUnosDTO.Zahtjev uDto(Sablon s) {
        return new SpisakUnosDTO.Zahtjev(
                s.kljuc(),
                s.naziv(),
                bezLinkova(s.opis()),
                s.vrste().stream().map(v -> new SpisakUnosDTO.Vrsta(v.getIdVrsta(), v.getNaziv())).toList(),
                s.potrebno(),
                s.poClanu(),
                s.sifreKriterija());
    }

    /** "[tekst](/adresa)" -> "tekst" */
    private static String bezLinkova(String opis) {
        return opis == null ? null : opis.replaceAll("\\[([^\\]]+)\\]\\([^)]*\\)", "$1").trim();
    }
}