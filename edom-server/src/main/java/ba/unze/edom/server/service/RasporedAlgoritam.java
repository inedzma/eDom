package ba.unze.edom.server.service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Pravila rasporeda u sobe, bez baze (lako se testira).
 *
 * 1. Mjesto dobijaju prvi s rang liste, posebno za muške i ženske sobe.
 * 2. Dvokrevetne sobe: prvo apsolventi, master i 4. godina i više, pa 3. godina, pa ostali;
 *    unutar grupe po rang listi. Ko je tražio trokrevetnu, ne ide u dvokrevetnu.
 * 3. Obostrana želja za cimerom (oba su naveli jedno drugo) stavlja ih u istu sobu kad god ima mjesta.
 * 4. Željena soba se dodjeljuje ako je slobodna i odgovara spolu.
 * 5. Ostali popunjavaju preostala mjesta po rang listi; prvo se dopunjavaju započete sobe.
 */
public final class RasporedAlgoritam {

    /**
     * Student koji čeka sobu.
     * godina: 7 = apsolvent, 8 = master/postdiplomac
     * zeljeniKapacitet: 2, 3 ili null (svejedno)
     * cimeri: id prijava koje je naveo kao cimere
     */
    public record Kandidat(int idPrijava, String spol, int godina, int rang,
                           Integer zeljeniKapacitet, Integer zeljenaSoba, Set<Integer> cimeri) {}

    /** Soba sa slobodnim mjestima; spol null = još nije određen. */
    public static final class SobaStanje {
        final int idSoba;
        final int broj;
        final int kapacitet;
        String spol;
        int slobodno;

        public SobaStanje(int idSoba, int broj, int kapacitet, String spol, int slobodno) {
            this.idSoba = idSoba;
            this.broj = broj;
            this.kapacitet = kapacitet;
            this.spol = spol;
            this.slobodno = slobodno;
        }

        boolean zaSpol(String s) {
            return spol == null || spol.equals(s);
        }

        boolean zapoceta() {
            return slobodno < kapacitet;
        }
    }

    /** idSoba null = nema mjesta. */
    public record Dodjela(int idPrijava, Integer idSoba, Integer brojSobe, int kapacitetSobe,
                          String razlog, boolean zeljaIspunjena) {}

    private final Map<Integer, Kandidat> kandidati = new LinkedHashMap<>();
    private final List<SobaStanje> sobe;
    private final Map<Integer, Dodjela> dodjele = new LinkedHashMap<>();
    private Set<Integer> izabrani = Set.of();

    private RasporedAlgoritam(List<Kandidat> k, List<SobaStanje> s) {
        k.stream().sorted(Comparator.comparingInt(Kandidat::rang))
                .forEach(x -> kandidati.put(x.idPrijava(), x));
        this.sobe = s.stream().sorted(Comparator.comparingInt(x -> x.broj)).collect(Collectors.toList());
    }

    /** Dodjela za svakog kandidata, redom po rang listi. */
    public static List<Dodjela> rasporedi(List<Kandidat> kandidati, List<SobaStanje> sobe) {
        RasporedAlgoritam a = new RasporedAlgoritam(kandidati, sobe);
        a.izvrsi();
        return a.kandidati.keySet().stream().map(a.dodjele::get).toList();
    }

    // ---------------------------------------------------------------------

    private void izvrsi() {
        izabrani = izaberiPoKapacitetu();

        // 1) dvokrevetne, po grupama prioriteta
        List<Kandidat> zaDvokrevetne = kandidati.values().stream()
                .filter(k -> izabrani.contains(k.idPrijava()))
                .filter(k -> !Integer.valueOf(3).equals(k.zeljeniKapacitet()))
                .sorted(Comparator.comparingInt((Kandidat k) -> grupa(k.godina()))
                        .thenComparingInt(Kandidat::rang))
                .toList();

        for (Kandidat k : zaDvokrevetne) {
            if (dodjele.containsKey(k.idPrijava())) continue;
            if (nemaSlobodnih(2, k.spol())) continue;
            smjestiUDvokrevetnu(k);
        }

        // 2) svi ostali izabrani, po rang listi
        for (Kandidat k : kandidati.values()) {
            if (!izabrani.contains(k.idPrijava()) || dodjele.containsKey(k.idPrijava())) continue;
            smjestiOstale(k);
        }

        // 3) ko nije izabran ili nije stao
        for (Kandidat k : kandidati.values()) {
            if (dodjele.containsKey(k.idPrijava())) continue;
            String razlog = izabrani.contains(k.idPrijava())
                    ? "Nema slobodnog mjesta koje odgovara (" + nazivSpola(k.spol()) + ")."
                    : "Izvan kapaciteta: sve " + nazivSpola(k.spol()) + " mjesta su popunjena boljim s rang liste.";
            dodjele.put(k.idPrijava(), new Dodjela(k.idPrijava(), null, null, 0, razlog, false));
        }
    }

    /** Prvih N po rang listi za svaki spol, gdje je N broj slobodnih mjesta za taj spol. */
    private Set<Integer> izaberiPoKapacitetu() {
        int slobodnaNeodredjena = sobe.stream().filter(s -> s.spol == null).mapToInt(s -> s.slobodno).sum();
        Map<String, Integer> mjesta = new HashMap<>();
        for (SobaStanje s : sobe) {
            if (s.spol != null) mjesta.merge(s.spol, s.slobodno, Integer::sum);
        }
        Set<Integer> izabrani = new HashSet<>();
        Map<String, Integer> uzeto = new HashMap<>();
        int uzetoNeodredjenih = 0;
        for (Kandidat k : kandidati.values()) {
            int svoja = mjesta.getOrDefault(k.spol(), 0);
            int vec = uzeto.getOrDefault(k.spol(), 0);
            if (vec < svoja) {
                uzeto.merge(k.spol(), 1, Integer::sum);
                izabrani.add(k.idPrijava());
            } else if (uzetoNeodredjenih < slobodnaNeodredjena) {
                uzetoNeodredjenih++;
                izabrani.add(k.idPrijava());
            }
        }
        return izabrani;
    }

    private void smjestiUDvokrevetnu(Kandidat k) {
        String grupaTekst = opisGrupe(k.godina());

        // tražio je konkretnu sobu koja nije dvokrevetna: poštujemo to u drugom krugu
        if (k.zeljenaSoba() != null && sobe.stream()
                .anyMatch(s -> s.idSoba == k.zeljenaSoba() && s.kapacitet != 2)) {
            return;
        }

        // obostrana želja: oboje u praznu dvokrevetnu
        Optional<Kandidat> cimer = obostraniCimeri(k).stream()
                .filter(c -> !Integer.valueOf(3).equals(c.zeljeniKapacitet()))
                .findFirst();
        if (cimer.isPresent()) {
            Optional<SobaStanje> prazna = sobe.stream()
                    .filter(s -> s.kapacitet == 2 && s.slobodno >= 2 && s.zaSpol(k.spol()))
                    .findFirst();
            if (prazna.isPresent()) {
                String r = "Dvokrevetna (" + grupaTekst + ", " + k.rang() + ". na rang listi), obostrana želja za cimerom";
                smjesti(k, prazna.get(), r, true);
                smjesti(cimer.get(), prazna.get(), "Dvokrevetna, obostrana želja za cimerom", true);
                return;
            }
            // nema prazne dvokrevetne za oboje: ne razdvajamo ih, idu zajedno u trokrevetnu
            return;
        }

        // željena dvokrevetna soba
        SobaStanje zeljena = zeljenaAkoSlobodna(k);
        if (zeljena != null && zeljena.kapacitet == 2) {
            smjesti(k, zeljena, "Željena soba (" + grupaTekst + ", " + k.rang() + ". na rang listi)", true);
            return;
        }

        SobaStanje s = najboljaSoba(k.spol(), 2);
        if (s != null) {
            boolean zelja = Integer.valueOf(2).equals(k.zeljeniKapacitet());
            smjesti(k, s, "Dvokrevetna: " + grupaTekst + ", " + k.rang() + ". na rang listi", zelja);
        }
    }

    private void smjestiOstale(Kandidat k) {
        // obostrani cimeri koji još nisu smješteni: svi zajedno
        List<Kandidat> grupa = new ArrayList<>();
        grupa.add(k);
        obostraniCimeri(k).stream().filter(c -> !dodjele.containsKey(c.idPrijava())).forEach(grupa::add);
        if (grupa.size() > 1) {
            Optional<SobaStanje> zajedno = sobe.stream()
                    .filter(s -> s.slobodno >= grupa.size() && s.zaSpol(k.spol()))
                    .min(Comparator.comparingInt((SobaStanje s) -> s.kapacitet == 3 ? 0 : 1)
                            .thenComparingInt(s -> s.zapoceta() ? 0 : 1)
                            .thenComparingInt(s -> s.broj));
            if (zajedno.isPresent()) {
                for (Kandidat c : grupa) {
                    smjesti(c, zajedno.get(), "Obostrana želja za cimerom, " + c.rang() + ". na rang listi", true);
                }
                return;
            }
        }

        SobaStanje zeljena = zeljenaAkoSlobodna(k);
        if (zeljena != null) {
            smjesti(k, zeljena, "Željena soba, " + k.rang() + ". na rang listi", true);
            return;
        }

        SobaStanje s = najboljaSoba(k.spol(), 3);
        if (s == null) s = najboljaSoba(k.spol(), null);   // nema trokrevetnih: bilo koja
        if (s != null) {
            boolean zelja = k.zeljeniKapacitet() != null && k.zeljeniKapacitet() == s.kapacitet;
            String r = (s.kapacitet == 2 ? "Dvokrevetna (ostalo slobodno)" : "Trokrevetna")
                    + ", " + k.rang() + ". na rang listi";
            smjesti(k, s, r, zelja);
        }
    }

    // ---------------------------------------------------------------------

    /** Kandidati koje je k naveo, a koji su naveli i njega, istog spola. */
    private List<Kandidat> obostraniCimeri(Kandidat k) {
        if (k.cimeri() == null) return List.of();
        return k.cimeri().stream()
                .map(kandidati::get)
                .filter(Objects::nonNull)
                .filter(c -> c.cimeri() != null && c.cimeri().contains(k.idPrijava()))
                .filter(c -> Objects.equals(c.spol(), k.spol()))
                .filter(c -> izabrani.contains(c.idPrijava()))
                .filter(c -> !dodjele.containsKey(c.idPrijava()))
                .sorted(Comparator.comparingInt(Kandidat::rang))
                .toList();
    }

    private SobaStanje zeljenaAkoSlobodna(Kandidat k) {
        if (k.zeljenaSoba() == null) return null;
        return sobe.stream()
                .filter(s -> s.idSoba == k.zeljenaSoba() && s.slobodno > 0 && s.zaSpol(k.spol()))
                .findFirst().orElse(null);
    }

    /** Započete sobe prvo (da se ne razbacuju studenti), pa prazne; po broju sobe. */
    private SobaStanje najboljaSoba(String spol, Integer kapacitet) {
        return sobe.stream()
                .filter(s -> kapacitet == null || s.kapacitet == kapacitet)
                .filter(s -> s.slobodno > 0 && s.zaSpol(spol))
                .min(Comparator.comparingInt((SobaStanje s) -> Objects.equals(s.spol, spol) ? 0 : 1)
                        .thenComparingInt(s -> s.zapoceta() ? 0 : 1)
                        .thenComparingInt(s -> s.broj))
                .orElse(null);
    }

    private boolean nemaSlobodnih(int kapacitet, String spol) {
        return sobe.stream().noneMatch(s -> s.kapacitet == kapacitet && s.slobodno > 0 && s.zaSpol(spol));
    }

    private void smjesti(Kandidat k, SobaStanje s, String razlog, boolean zelja) {
        s.slobodno--;
        if (s.spol == null) s.spol = k.spol();
        dodjele.put(k.idPrijava(), new Dodjela(k.idPrijava(), s.idSoba, s.broj, s.kapacitet, razlog, zelja));
    }

    /** 1 = apsolvent, master, 4. godina i više; 2 = 3. godina; 3 = ostali. */
    static int grupa(int godina) {
        if (godina >= 4) return 1;
        if (godina == 3) return 2;
        return 3;
    }

    private static String opisGrupe(int godina) {
        if (godina == 7) return "apsolvent";
        if (godina == 8) return "master";
        return godina + ". godina";
    }

    private static String nazivSpola(String s) {
        return "M".equals(s) ? "muška" : "Z".equals(s) ? "ženska" : "neodređena";
    }
}