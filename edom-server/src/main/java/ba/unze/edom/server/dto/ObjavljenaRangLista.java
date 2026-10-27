package ba.unze.edom.server.dto;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

/** Rang lista kakva je bila u trenutku objave. Snima se u fajl, web je samo čita. */
public record ObjavljenaRangLista(
        String vrsta,              // PRELIMINARNA / KONACNA
        String objavljeno,         // "22.10.2026 17:45"
        String objavio,
        String akademskaGodina,    // "2026/2027"
        int kapacitet,
        int slobodno,
        List<Stavka> brucosi,
        List<Stavka> saUslovom,
        List<Stavka> listaCekanja,
        LocalDate useljenjeOd,     // može biti null (termin još nije poznat)
        LocalDate useljenjeDo
) implements Serializable {

    private static final DateTimeFormatter D = DateTimeFormatter.ofPattern("dd.MM.yyyy.");
    private static final String[] MJESECI = {"januar", "februar", "mart", "april", "maj", "juni",
            "juli", "august", "septembar", "oktobar", "novembar", "decembar"};

    public record Stavka(
            int redniBroj,
            Integer idStudent,     // da se prijavljenom studentu istakne red; ne prikazuje se
            String sifra,
            double bodovi,
            boolean primljen
    ) implements Serializable {}

    public int brojStudenata() {
        return brucosi.size() + saUslovom.size() + listaCekanja.size();
    }

    public long primljenoBrucosa() {
        return brucosi.stream().filter(Stavka::primljen).count();
    }

    public long primljenoVisih() {
        return saUslovom.stream().filter(Stavka::primljen).count();
    }

    /** "27.10.2026. – 04.11.2026." ili null. */
    public String useljenje() {
        if (useljenjeOd == null || useljenjeDo == null) return null;
        return useljenjeOd.format(D) + " – " + useljenjeDo.format(D);
    }

    /** Mjesec za koji se uplaćuje smještaj (mjesec početka useljenja). */
    public String mjesecUplate() {
        return useljenjeOd != null ? MJESECI[useljenjeOd.getMonthValue() - 1] : "oktobar";
    }

    /** Posljednji dan za žalbu: 7 dana od objave. */
    public String zalbeDo() {
        try {
            LocalDate d = LocalDate.parse(objavljeno.substring(0, 10), DateTimeFormatter.ofPattern("dd.MM.yyyy"));
            return d.plusDays(7).format(D);
        } catch (Exception e) {
            return null;
        }
    }

    public boolean preliminarna() {
        return !"KONACNA".equals(vrsta);
    }
}