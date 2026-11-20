package ba.unze.edom.server.service.uplate;

import ba.unze.edom.common.dto.uplate.UplateTabelaDTO;
import ba.unze.edom.common.dto.uplate.UplateTabelaDTO.Celija;
import ba.unze.edom.common.dto.uplate.UplateTabelaDTO.Red;
import ba.unze.edom.server.entity.Smjestaj;
import ba.unze.edom.server.entity.Student;
import ba.unze.edom.server.entity.Uplata;
import ba.unze.edom.server.exception.PrijavaException;
import ba.unze.edom.server.repository.StudentRepository;
import ba.unze.edom.server.repository.UplataRepository;
import ba.unze.edom.server.service.obavijesti.EmailService;
import ba.unze.edom.server.service.opste.PostavkeService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.*;

/**
 * Evidencija mjesečnih uplata za smještaj.
 *
 * Čuva se samo ono što je plaćeno. Šta je student dužan računa se iz smještaja:
 * svaki mjesec od useljenja do iseljenja (najviše do jula), koji je već prošao,
 * a za koji nema uplate.
 */
@Service
@RequiredArgsConstructor
public class UplataService {

    private static final ZoneId ZONA = ZoneId.of("Europe/Sarajevo");

    /** Akademska godina za smještaj: oktobar–juli. */
    private static final int PRVI_MJESEC = 10;
    private static final int ZADNJI_MJESEC = 7;

    private static final String[] MJESECI = {"", "januar", "februar", "mart", "april", "maj", "juni",
            "juli", "august", "septembar", "oktobar", "novembar", "decembar"};

    private final UplataRepository uplataRepository;
    private final StudentRepository studentRepository;
    private final PostavkeService postavke;
    private final EmailService emailService;

    @PersistenceContext
    private EntityManager em;

    // ---------- tabela za desktop ----------

    @Transactional(readOnly = true)
    public UplateTabelaDTO tabela(Integer godina) {
        int g = godina != null ? godina : postavke.akademskaGodina();
        List<YearMonth> mjeseci = mjeseciGodine(g);
        YearMonth danas = YearMonth.now(ZONA);

        // ko je te godine stvarno bio u domu (useljen ili već iseljen)
        List<Smjestaj> smjestaji = em.createQuery(
                        "SELECT sm FROM Smjestaj sm JOIN FETCH sm.prijava p JOIN FETCH p.student JOIN FETCH sm.soba "
                                + "WHERE p.akademskaGodina = :g AND sm.status IN :statusi AND sm.datumUseljenja IS NOT NULL",
                        Smjestaj.class)
                .setParameter("g", g)
                .setParameter("statusi", List.of(Smjestaj.USELJEN, Smjestaj.ISELJEN))
                .getResultList();

        // student može promijeniti sobu: od prvog useljenja do zadnjeg iseljenja
        Map<Integer, Boravak> boravci = new LinkedHashMap<>();
        for (Smjestaj sm : smjestaji) {
            boravci.computeIfAbsent(sm.getPrijava().getStudent().getIdStudent(),
                    id -> new Boravak(sm.getPrijava().getStudent())).dodaj(sm);
        }

        // plaćeni mjeseci: student → mjesec → datum uplate
        Map<Integer, Map<YearMonth, LocalDate>> placeno = new HashMap<>();
        for (Uplata u : uplataRepository.findByMjesecBetween(
                mjeseci.get(0).atDay(1), mjeseci.get(mjeseci.size() - 1).atDay(1))) {
            placeno.computeIfAbsent(u.getStudent().getIdStudent(), id -> new HashMap<>())
                    .put(YearMonth.from(u.getMjesec()), u.getDatumUplate());
        }

        List<Red> redovi = boravci.values().stream()
                .sorted(Comparator.comparing((Boravak b) -> nizak(b.student.getPrezime()))
                        .thenComparing(b -> nizak(b.student.getIme())))
                .map(b -> red(b, mjeseci, placeno.getOrDefault(b.student.getIdStudent(), Map.of()), danas))
                .toList();

        return new UplateTabelaDTO(g, mjeseci.stream().map(YearMonth::toString).toList(), redovi);
    }

    private Red red(Boravak b, List<YearMonth> mjeseci, Map<YearMonth, LocalDate> placeno, YearMonth danas) {
        List<Celija> celije = new ArrayList<>();
        int dug = 0;

        for (YearMonth m : mjeseci) {
            String stanje;
            LocalDate datum = placeno.get(m);
            if (datum != null) {
                stanje = UplateTabelaDTO.PLACENO;
            } else if (!b.bioUDomu(m)) {
                stanje = UplateTabelaDTO.NIJE_U_DOMU;
            } else if (m.isBefore(danas)) {
                stanje = UplateTabelaDTO.DUG;
                dug++;
            } else {
                stanje = UplateTabelaDTO.CEKA;
            }
            celije.add(new Celija(m.toString(), stanje, datum));
        }

        Student s = b.student;
        return new Red(s.getIdStudent(), s.getPrezime() + " " + s.getIme(), b.soba, celije, dug);
    }

    // ---------- za studenta (web, Moj smještaj) ----------

    /**
     * Uplate prijavljenog studenta u tekućoj akademskoj godini: mjeseci od useljenja
     * do tekućeg mjeseca. Rok za uplatu je kraj mjeseca (poslije toga mjesec je dug,
     * isto kao u tabeli na desktopu). Prazno ako student ove godine nije useljen.
     */
    @Transactional(readOnly = true)
    public Optional<MojeUplate> zaStudenta(Integer idStudenta) {
        if (idStudenta == null) return Optional.empty();
        UplateTabelaDTO t = tabela(null);
        YearMonth danas = YearMonth.now(ZONA);

        return t.redovi().stream()
                .filter(r -> r.idStudent() == idStudenta)
                .findFirst()
                .map(r -> {
                    List<MjesecUplate> mjeseci = r.celije().stream()
                            .filter(c -> !UplateTabelaDTO.NIJE_U_DOMU.equals(c.stanje()))
                            .filter(c -> !YearMonth.parse(c.mjesec()).isAfter(danas))
                            .map(c -> {
                                YearMonth m = YearMonth.parse(c.mjesec());
                                return new MjesecUplate(naziv(m), c.stanje(), c.datum(), m.atEndOfMonth());
                            })
                            .toList();
                    MjesecUplate sljedeci = mjeseci.stream()
                            .filter(m -> UplateTabelaDTO.CEKA.equals(m.stanje()))
                            .findFirst().orElse(null);
                    return new MojeUplate(t.akademskaGodina(), mjeseci, r.dug(), sljedeci);
                });
    }

    /** Stanje uplata jednog studenta za stranicu Moj smještaj. */
    public record MojeUplate(int akademskaGodina, List<MjesecUplate> mjeseci, int dug, MjesecUplate sljedeci) {
        public boolean imaDug() { return dug > 0; }

        /** "oktobar 2026, novembar 2026" */
        public String dugMjeseci() {
            return String.join(", ", mjeseci.stream().filter(MjesecUplate::nijePlaceno).map(MjesecUplate::naziv).toList());
        }
    }

    /** @param rok zadnji dan mjeseca do kojeg treba platiti */
    public record MjesecUplate(String naziv, String stanje, LocalDate datumUplate, LocalDate rok) {
        public boolean placeno()    { return UplateTabelaDTO.PLACENO.equals(stanje); }
        public boolean nijePlaceno() { return UplateTabelaDTO.DUG.equals(stanje); }
        public boolean ceka()       { return UplateTabelaDTO.CEKA.equals(stanje); }
    }

    // ---------- evidencija ----------

    /** Označi mjesec kao plaćen; ako je već označen, samo se ispravi datum. */
    @Transactional
    public void evidentiraj(Integer idStudenta, YearMonth mjesec, LocalDate datumUplate) {
        Student s = studentRepository.findById(idStudenta)
                .orElseThrow(() -> new PrijavaException("Student ne postoji."));
        LocalDate datum = datumUplate != null ? datumUplate : LocalDate.now(ZONA);
        if (datum.isAfter(LocalDate.now(ZONA))) {
            throw new PrijavaException("Datum uplate ne može biti u budućnosti.");
        }

        Uplata u = uplataRepository.findByStudent_IdStudentAndMjesec(idStudenta, mjesec.atDay(1))
                .orElseGet(Uplata::new);
        u.setStudent(s);
        u.setMjesec(mjesec.atDay(1));
        u.setDatumUplate(datum);
        uplataRepository.save(u);
    }

    /** Poništava pogrešno unesenu uplatu. */
    @Transactional
    public void ponisti(Integer idStudenta, YearMonth mjesec) {
        uplataRepository.findByStudent_IdStudentAndMjesec(idStudenta, mjesec.atDay(1))
                .ifPresent(uplataRepository::delete);
    }

    // ---------- podsjetnik e-mailom ----------

    /**
     * Šalje svakom navedenom studentu spisak mjeseci koje duguje.
     * Studenti koji ništa ne duguju ili nemaju e-mail se preskaču.
     *
     * @return poruke o onome što nije poslano (prazno = sve poslano)
     */
    public PodsjetnikRezultat posaljiPodsjetnik(int godina, List<Integer> idStudenti, String napomena) {
        Map<Integer, Red> poStudentu = new HashMap<>();
        for (Red r : tabela(godina).redovi()) poStudentu.put(r.idStudent(), r);

        String nap = napomena != null && !napomena.isBlank() ? napomena.trim() : null;
        int poslano = 0;
        List<String> nijePoslano = new ArrayList<>();

        for (Student s : studentRepository.findAllById(idStudenti)) {
            String ko = s.getIme() + " " + s.getPrezime();
            Red r = poStudentu.get(s.getIdStudent());
            List<String> dug = r == null ? List.of() : r.celije().stream()
                    .filter(c -> UplateTabelaDTO.DUG.equals(c.stanje()))
                    .map(c -> naziv(YearMonth.parse(c.mjesec())))
                    .toList();

            if (dug.isEmpty()) { nijePoslano.add(ko + ": nema neplaćenih mjeseci"); continue; }
            if (s.getEmail() == null || s.getEmail().isBlank()) { nijePoslano.add(ko + ": nema e-mail adresu"); continue; }

            try {
                emailService.posalji(s.getEmail(), "E-Dom: podsjetnik o uplati za smještaj",
                        tekstPodsjetnika(s.getIme(), godina, dug, nap));
                poslano++;
            } catch (RuntimeException e) {
                nijePoslano.add(ko + ": slanje nije uspjelo");
            }
        }
        return new PodsjetnikRezultat(poslano, nijePoslano);
    }

    public record PodsjetnikRezultat(int poslano, List<String> nijePoslano) {}

    private static String tekstPodsjetnika(String ime, int godina, List<String> mjeseci, String napomena) {
        StringBuilder t = new StringBuilder()
                .append("Poštovani/a ").append(ime).append(",\n\n")
                .append("prema evidenciji Studentskog centra Univerziteta u Zenici, za akademsku ")
                .append(godina).append("/").append(godina + 1)
                .append(". godinu nisu evidentirane uplate za smještaj za ")
                .append(mjeseci.size() == 1 ? "mjesec:\n\n" : "mjesece:\n\n");
        mjeseci.forEach(m -> t.append("  • ").append(m).append("\n"));
        t.append("\n");
        if (napomena != null) t.append(napomena).append("\n\n");
        t.append("Molimo da uplatu izvršite u najkraćem roku. Ako ste uplatu već izvršili, ")
                .append("dostavite dokaz o uplati administraciji doma.\n\n")
                .append("Studentski centar Zenica");
        return t.toString();
    }

    /** "oktobar 2026" */
    private static String naziv(YearMonth m) {
        return MJESECI[m.getMonthValue()] + " " + m.getYear();
    }

    // ---------- pomoćne ----------

    /** Oktobar godine g … juli godine g+1. */
    static List<YearMonth> mjeseciGodine(int g) {
        List<YearMonth> l = new ArrayList<>();
        for (YearMonth m = YearMonth.of(g, PRVI_MJESEC); !m.isAfter(YearMonth.of(g + 1, ZADNJI_MJESEC)); m = m.plusMonths(1)) {
            l.add(m);
        }
        return l;
    }

    private static String nizak(String s) {
        return s == null ? "" : s.toLowerCase(Locale.ROOT);
    }

    /** Od kad do kad je student bio u domu te godine. */
    private static final class Boravak {
        final Student student;
        YearMonth od;
        YearMonth doMjeseca;      // null = još je u domu
        boolean jos;
        Integer soba;

        Boravak(Student student) {
            this.student = student;
        }

        void dodaj(Smjestaj sm) {
            YearMonth useljen = YearMonth.from(sm.getDatumUseljenja());
            if (od == null || useljen.isBefore(od)) od = useljen;

            if (Smjestaj.USELJEN.equals(sm.getStatus())) {
                jos = true;
                soba = sm.getSoba().getBroj();
            } else if (sm.getDatumIseljenja() != null) {
                YearMonth iseljen = YearMonth.from(sm.getDatumIseljenja());
                if (doMjeseca == null || iseljen.isAfter(doMjeseca)) doMjeseca = iseljen;
                if (soba == null) soba = sm.getSoba().getBroj();
            }
        }

        /** Mjesec useljenja i mjesec iseljenja se plaćaju. */
        boolean bioUDomu(YearMonth m) {
            if (m.isBefore(od)) return false;
            return jos || doMjeseca == null || !m.isAfter(doMjeseca);
        }
    }
}