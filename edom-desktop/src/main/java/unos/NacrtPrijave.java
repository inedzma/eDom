package unos;

import ba.unze.edom.common.dto.dokumenti.DokumentUnosDTO;
import ba.unze.edom.common.dto.opste.SifarniciDTO;
import ba.unze.edom.common.dto.prijava.KriterijUnosDTO;
import ba.unze.edom.common.dto.prijava.PrijavaUnosDTO;
import ba.unze.edom.common.dto.prijava.RucniUnosDTO;
import ba.unze.edom.common.dto.prijava.SpisakUnosDTO;
import ba.unze.edom.common.dto.student.StudentUnosDTO;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

/** Podaci koje admin unosi kroz pet koraka; serveru se šalju tek na kraju. */
public class NacrtPrijave {

    public SifarniciDTO sifarnici;

    // ---------- 1. student i studij ----------
    public StudentUnosDTO student;
    public Integer idPostojecegStudenta;
    public boolean imaNalog;
    public Integer akademskaGodina;
    public BigDecimal prosjek;
    public Integer polozeniIspiti;
    public Integer upisGodinePut = 1;

    // ---------- 2. domaćinstvo ----------
    public Integer brojClanova;
    public BigDecimal udaljenostKm;
    /** Ostali članovi domaćinstva, bez studenta. */
    public final List<Clan> clanovi = new ArrayList<>();

    // ---------- 3. kriteriji ----------
    /** šifra kriterija -> parametar (null kad kriterij ne traži parametar) */
    public final Map<String, BigDecimal> kriteriji = new LinkedHashMap<>();
    /** šifra kriterija -> period (samo za kriterije s periodom, npr. OS RBiH) */
    public final Map<String, Period> periodi = new HashMap<>();

    // ---------- 4. dokumenti ----------
    public SpisakUnosDTO spisak;
    /** Za koje podatke je spisak učitan (da znamo treba li ga ponovo tražiti). */
    public String spisakZa;
    /** Dostavljeni dokumenti: "kljucReda|idVrste". */
    public final Set<String> dostavljeno = new HashSet<>();
    /** Članovi: redni broj člana (0..) -> vrsta dokumenta koji je donio. */
    public final Map<Integer, Integer> dokumentClana = new HashMap<>();
    /** Kriteriji bez posebnog dokumenta koje je uposlenik provjerio. */
    public final Set<String> provjereniKriteriji = new HashSet<>();

    // ---------- 5. pregled ----------
    public String napomena;

    public record Clan(String ime, BigDecimal primanja) {}

    public record Period(LocalDate od, LocalDate doDatuma) {}

    // ---------- pomoćne ----------

    public boolean brucos() {
        return student != null && Integer.valueOf(1).equals(student.godinaStudija());
    }

    public BigDecimal ukupnaPrimanja() {
        return clanovi.stream()
                .map(c -> c.primanja() != null ? c.primanja() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /** Ime člana za prikaz: upisano ime ili "Član 2". */
    public String imeClana(int i) {
        Clan c = i < clanovi.size() ? clanovi.get(i) : null;
        return c != null && c.ime() != null && !c.ime().isBlank() ? c.ime().trim() : "Član " + (i + 1);
    }

    /** Spisak zavisi od godine studija, broja članova i kriterija. */
    public String kljucSpiska() {
        List<String> sifre = new ArrayList<>(kriteriji.keySet());
        Collections.sort(sifre);
        return student.godinaStudija() + "|" + brojClanova + "|" + String.join(",", sifre);
    }

    public static String kljucVrste(SpisakUnosDTO.Zahtjev z, Integer idVrste) {
        return z.kljuc() + "|" + idVrste;
    }

    /** Je li red spiska ispunjen: bilo koja vrsta dostavljena, a po članu svi članovi. */
    public boolean ispunjen(SpisakUnosDTO.Zahtjev z) {
        if (z.poClanu()) {
            for (int i = 0; i < z.potrebno(); i++) {
                if (!dokumentClana.containsKey(i)) return false;
            }
            return true;
        }
        return z.vrste().stream().anyMatch(v -> dostavljeno.contains(kljucVrste(z, v.id())));
    }

    public int falaObaveznih() {
        return spisak == null ? 0 : (int) spisak.obavezni().stream().filter(z -> !ispunjen(z)).count();
    }

    /** Sve što je uneseno, u obliku koji server prima (za pregled bodova i za podnošenje). */
    public RucniUnosDTO zahtjev() {
        List<DokumentUnosDTO> dokumenti = new ArrayList<>();
        Set<String> dokazaniKriteriji = new HashSet<>(provjereniKriteriji);
        boolean uspjeh = false, cips = false, kucna = false, clanoviDokumentovani = true;

        if (spisak != null) {
            List<SpisakUnosDTO.Zahtjev> redovi = new ArrayList<>(spisak.obavezni());
            redovi.addAll(spisak.dodatni());

            for (SpisakUnosDTO.Zahtjev z : redovi) {
                if (z.poClanu()) {
                    for (int i = 0; i < z.potrebno(); i++) {
                        Integer idVrste = dokumentClana.get(i);
                        if (idVrste == null) {
                            clanoviDokumentovani = false;
                            continue;
                        }
                        String naziv = z.vrste().stream()
                                .filter(v -> v.id().equals(idVrste))
                                .map(SpisakUnosDTO.Vrsta::naziv)
                                .findFirst().orElse("Dokument");
                        dokumenti.add(new DokumentUnosDTO(idVrste, naziv + " - " + imeClana(i), true, null));
                    }
                    continue;
                }

                boolean ima = false;
                for (SpisakUnosDTO.Vrsta v : z.vrste()) {
                    if (dostavljeno.contains(kljucVrste(z, v.id()))) {
                        dokumenti.add(new DokumentUnosDTO(v.id(), null, true, null));
                        ima = true;
                    }
                }
                if (!ima) continue;

                switch (z.kljuc()) {
                    case "uspjeh" -> uspjeh = true;
                    case "cips" -> cips = true;
                    case "kucna" -> kucna = true;
                    default -> { }
                }
                if (z.sifreKriterija() != null) dokazaniKriteriji.addAll(z.sifreKriterija());
            }
        }

        List<KriterijUnosDTO> kr = new ArrayList<>();
        kriteriji.forEach((sifra, parametar) -> {
            Period p = periodi.get(sifra);
            kr.add(new KriterijUnosDTO(sifra, parametar, dokazaniKriteriji.contains(sifra),
                    p != null ? p.od() : null, p != null ? p.doDatuma() : null));
        });

        PrijavaUnosDTO prijava = new PrijavaUnosDTO(
                akademskaGodina, prosjek, brucos() ? null : polozeniIspiti,
                brojClanova, ukupnaPrimanja(), udaljenostKm, napomena,
                uspjeh,                              // uspjeh verifikovan
                cips,                                // udaljenost verifikovana
                kucna && clanoviDokumentovani,       // primanja verifikovana
                upisGodinePut);

        return new RucniUnosDTO(student, prijava, kr, dokumenti);
    }
}