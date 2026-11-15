package ba.unze.edom.server.service.sobe;

import ba.unze.edom.server.service.sobe.RasporedAlgoritam.Dodjela;
import ba.unze.edom.server.service.sobe.RasporedAlgoritam.Kandidat;
import ba.unze.edom.server.service.sobe.RasporedAlgoritam.SobaStanje;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Prijedlog rasporeda u sobe: spol sobe, kapacitet, prednost za dvokrevetne,
 * želje za sobom i cimerom. Bez baze i Springa.
 */
class RasporedAlgoritamTest {

    @Test
    @DisplayName("Studenti različitog spola ne dijele sobu")
    void spolSobe() {
        List<SobaStanje> sobe = List.of(soba(1, 101, 2, null, 2));
        List<Dodjela> d = RasporedAlgoritam.rasporedi(List.of(
                kandidat(1, "M", 1, 1),
                kandidat(2, "Z", 1, 2)), sobe);

        assertEquals(1, dodjela(d, 1).idSoba());
        assertNull(dodjela(d, 2).idSoba(), "Studentica ne smije u sobu gdje je već student");
    }

    @Test
    @DisplayName("Dvokrevetne prvo dobijaju studenti 4. i viših godina, pa 3. godina")
    void prednostZaDvokrevetne() {
        List<SobaStanje> sobe = List.of(
                soba(1, 101, 2, "M", 2),
                soba(2, 102, 3, "M", 3));
        List<Dodjela> d = RasporedAlgoritam.rasporedi(List.of(
                        kandidat(1, "M", 1, 1),     // brucoš, najbolji na rang listi
                        kandidat(2, "M", 4, 2),     // 4. godina
                        kandidat(3, "M", 3, 3)),    // 3. godina
                sobe);

        assertEquals(1, dodjela(d, 2).idSoba());
        assertEquals(1, dodjela(d, 3).idSoba());
        assertEquals(2, dodjela(d, 1).idSoba(), "Brucoš ide u trokrevetnu iako je prvi na rang listi");
    }

    @Test
    @DisplayName("Ko je tražio trokrevetnu ne ide u dvokrevetnu")
    void zeljaZaTrokrevetnom() {
        List<SobaStanje> sobe = List.of(
                soba(1, 101, 2, "M", 2),
                soba(2, 102, 3, "M", 3));
        List<Dodjela> d = RasporedAlgoritam.rasporedi(List.of(
                new Kandidat(1, "M", 4, 1, 3, null, Set.of()),
                kandidat(2, "M", 1, 2)), sobe);

        assertEquals(2, dodjela(d, 1).idSoba());
        assertTrue(dodjela(d, 1).zeljaIspunjena());
        assertEquals(1, dodjela(d, 2).idSoba());
    }

    @Test
    @DisplayName("Obostrana želja za cimerom: oba studenta u istoj sobi")
    void obostranaZeljaZaCimerom() {
        List<SobaStanje> sobe = List.of(
                soba(1, 101, 3, "M", 3),
                soba(2, 102, 3, "M", 3));
        List<Dodjela> d = RasporedAlgoritam.rasporedi(List.of(
                new Kandidat(1, "M", 2, 1, null, null, Set.of(3)),
                kandidat(2, "M", 2, 2),
                new Kandidat(3, "M", 2, 3, null, null, Set.of(1))), sobe);

        assertEquals(dodjela(d, 1).idSoba(), dodjela(d, 3).idSoba());
        assertTrue(dodjela(d, 3).razlog().contains("Obostrana"));
    }

    @Test
    @DisplayName("Jednostrana želja za cimerom se ne uzima u obzir")
    void jednostranaZelja() {
        List<SobaStanje> sobe = List.of(
                soba(1, 101, 3, "M", 1),
                soba(2, 102, 3, "M", 1));
        List<Dodjela> d = RasporedAlgoritam.rasporedi(List.of(
                new Kandidat(1, "M", 2, 1, null, null, Set.of(2)),   // navodi drugog
                kandidat(2, "M", 2, 2)), sobe);                      // drugi ne navodi njega

        assertNotEquals(dodjela(d, 1).idSoba(), dodjela(d, 2).idSoba());
    }

    @Test
    @DisplayName("Željena soba se dodjeljuje ako je slobodna")
    void zeljenaSoba() {
        List<SobaStanje> sobe = List.of(
                soba(1, 101, 3, "M", 3),
                soba(2, 102, 3, "M", 3));
        List<Dodjela> d = RasporedAlgoritam.rasporedi(List.of(
                new Kandidat(1, "M", 2, 1, null, 2, Set.of())), sobe);

        assertEquals(2, dodjela(d, 1).idSoba());
        assertTrue(dodjela(d, 1).zeljaIspunjena());
    }

    @Test
    @DisplayName("Prvo se popunjavaju započete sobe")
    void zapoceteSobePrvo() {
        List<SobaStanje> sobe = List.of(
                soba(1, 101, 3, "M", 3),    // prazna
                soba(2, 102, 3, "M", 1));   // već ima dva stanara
        List<Dodjela> d = RasporedAlgoritam.rasporedi(List.of(kandidat(1, "M", 2, 1)), sobe);

        assertEquals(2, dodjela(d, 1).idSoba());
    }

    @Test
    @DisplayName("Kad nema mjesta, sobu ne dobija onaj niže na rang listi")
    void izvanKapaciteta() {
        List<SobaStanje> sobe = List.of(soba(1, 101, 2, "M", 2));
        List<Dodjela> d = RasporedAlgoritam.rasporedi(List.of(
                kandidat(1, "M", 1, 1),
                kandidat(2, "M", 1, 2),
                kandidat(3, "M", 1, 3)), sobe);

        assertEquals(1, dodjela(d, 1).idSoba());
        assertEquals(1, dodjela(d, 2).idSoba());
        assertNull(dodjela(d, 3).idSoba());
        assertTrue(dodjela(d, 3).razlog().contains("Izvan kapaciteta"));
    }

    @Test
    @DisplayName("Svaki kandidat dobija tačno jednu stavku prijedloga")
    void svakiKandidatJednom() {
        List<SobaStanje> sobe = List.of(soba(1, 101, 3, null, 3));
        List<Dodjela> d = RasporedAlgoritam.rasporedi(List.of(
                kandidat(1, "M", 2, 1),
                kandidat(2, "Z", 2, 2),
                kandidat(3, "M", 2, 3)), sobe);

        assertEquals(3, d.size());
        assertEquals(3, d.stream().map(Dodjela::idPrijava).distinct().count());
    }

    @ParameterizedTest(name = "godina {0} → grupa {1}")
    @CsvSource({"8,1", "7,1", "6,1", "4,1", "3,2", "2,3", "1,3"})
    @DisplayName("Grupe prednosti za dvokrevetne sobe")
    void grupePrednosti(int godina, int grupa) {
        assertEquals(grupa, RasporedAlgoritam.grupa(godina));
    }

    // ---------- pomoćne ----------

    private static SobaStanje soba(int id, int broj, int kapacitet, String spol, int slobodno) {
        return new SobaStanje(id, broj, kapacitet, spol, slobodno);
    }

    private static Kandidat kandidat(int id, String spol, int godina, int rang) {
        return new Kandidat(id, spol, godina, rang, null, null, Set.of());
    }

    private static Dodjela dodjela(List<Dodjela> lista, int idPrijava) {
        return lista.stream().filter(x -> x.idPrijava() == idPrijava).findFirst().orElseThrow();
    }
}