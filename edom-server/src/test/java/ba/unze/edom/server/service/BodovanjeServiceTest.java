package ba.unze.edom.server.service;

import ba.unze.edom.server.entity.*;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

class BodovanjeServiceTest {

    private final BodovanjeService service = new BodovanjeService();

    private Dokument dokument(double bodovi, boolean dostavljen) {
        Dokument d = new Dokument();
        d.setBrojBodova(bodovi);
        d.setDostavljen(dostavljen);
        return d;
    }

    @Test
    @DisplayName("Prijava bez dokumenata ima 0 bodova")
    void prijavaBezDokumenata() {
        Prijava p = new Prijava();
        assertEquals(0.0, service.izracunajUkupneBodove(p));
    }

    @Test
    @DisplayName("Zbraja se samo ono sto je dostavljeno")
    void samoDostavljeni() {
        Prijava p = new Prijava();
        p.dodajDokument(dokument(10.0, true));
        p.dodajDokument(dokument(5.0, false));
        assertEquals(10.0, service.izracunajUkupneBodove(p));
    }

    @Test
    @DisplayName("Decimalni bodovi se ne odsijecaju")
    void decimalneVrijednosti() {
        Prijava p = new Prijava();
        p.dodajDokument(dokument(4.35, true));
        p.dodajDokument(dokument(8.35, true));
        // stara implementacija (int) bi ovdje vratila 12
        assertEquals(12.7, service.izracunajUkupneBodove(p), 0.0001);
    }

    @Test
    @DisplayName("Null prijava ne baca izuzetak")
    void nullPrijava() {
        assertEquals(0.0, service.izracunajUkupneBodove(null));
    }
}