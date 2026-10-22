package ba.unze.edom.server.dto;

import java.io.Serializable;
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
        List<Stavka> listaCekanja
) implements Serializable {

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
}