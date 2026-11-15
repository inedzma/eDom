package ba.unze.edom.server.service.rang;

import ba.unze.edom.common.dto.rang.KategorijaRanga;
import ba.unze.edom.common.dto.rang.RangListaAdminDTO;
import ba.unze.edom.common.dto.rang.RangRedDTO;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Rang lista: kvote za brucoše i više godine, granica prijema,
 * lista čekanja i redoslijed. Bez baze i Springa.
 */
class RangRaspodjelaTest {

    private final RangRaspodjela raspodjela = new RangRaspodjela();

    @Test
    @DisplayName("Kvote: 30% mjesta za brucoše, ostalo za više godine")
    void kvote() {
        List<RangRedDTO> redovi = new ArrayList<>();
        for (int i = 1; i <= 5; i++) redovi.add(brucos(i, 50 + i));
        for (int i = 11; i <= 20; i++) redovi.add(sUslovom(i, 60 + i));

        RangListaAdminDTO r = raspodjela.rasporedi("2026/2027", 2026, redovi, 10, 0.3, false);

        assertEquals(3, primljenih(r.brucosi()));
        assertEquals(7, primljenih(r.saUslovom()));
    }

    @Test
    @DisplayName("Primaju se oni s najviše bodova")
    void primajuSeNajbolji() {
        List<RangRedDTO> redovi = List.of(brucos(1, 40), brucos(2, 90), brucos(3, 70));

        RangListaAdminDTO r = raspodjela.rasporedi("2026/2027", 2026, redovi, 10, 0.2, false);

        // kvota brucoša je 2: primljeni su 90 i 70 bodova
        assertEquals(2, r.brucosi().get(0).idPrijava());
        assertEquals(3, r.brucosi().get(1).idPrijava());
        assertTrue(r.brucosi().get(0).primljen());
        assertTrue(r.brucosi().get(1).primljen());
        assertFalse(r.brucosi().get(2).primljen());
    }

    @Test
    @DisplayName("Neiskorištena brucoška mjesta dobijaju studenti viših godina")
    void neiskoristenaBrucoskaMjesta() {
        List<RangRedDTO> redovi = new ArrayList<>();
        redovi.add(brucos(1, 50));
        for (int i = 11; i <= 20; i++) redovi.add(sUslovom(i, 60 + i));

        RangListaAdminDTO r = raspodjela.rasporedi("2026/2027", 2026, redovi, 10, 0.3, false);

        assertEquals(1, primljenih(r.brucosi()));
        assertEquals(9, primljenih(r.saUslovom()));   // 7 + 2 neiskorištena
    }

    @Test
    @DisplayName("Slobodna mjesta se popunjavaju brucošima samo kad admin to uključi")
    void popuniBrucosima() {
        List<RangRedDTO> redovi = new ArrayList<>();
        for (int i = 1; i <= 5; i++) redovi.add(brucos(i, 50 + i));
        redovi.add(sUslovom(11, 80));
        redovi.add(sUslovom(12, 70));

        RangListaAdminDTO bez = raspodjela.rasporedi("2026/2027", 2026, redovi, 10, 0.3, false);
        RangListaAdminDTO sa = raspodjela.rasporedi("2026/2027", 2026, redovi, 10, 0.3, true);

        assertEquals(3, primljenih(bez.brucosi()));
        assertEquals(5, primljenih(sa.brucosi()));
    }

    @Test
    @DisplayName("Prijava s neispravnim podacima nikad nije primljena i ide na dno")
    void neispravniNaDno() {
        List<RangRedDTO> redovi = List.of(
                red(1, KategorijaRanga.BRUCOS, 99, "Prosjek izvan raspona", false),
                brucos(2, 40));

        RangListaAdminDTO r = raspodjela.rasporedi("2026/2027", 2026, redovi, 10, 0.5, false);

        assertEquals(2, r.brucosi().get(0).idPrijava());
        assertTrue(r.brucosi().get(0).primljen());
        assertEquals(1, r.brucosi().get(1).idPrijava());
        assertFalse(r.brucosi().get(1).primljen());
    }

    @Test
    @DisplayName("Na listi čekanja niko nije primljen")
    void listaCekanja() {
        List<RangRedDTO> redovi = List.of(
                red(1, KategorijaRanga.LISTA_CEKANJA, 120, null, false),
                red(2, KategorijaRanga.LISTA_CEKANJA, 100, null, false));

        RangListaAdminDTO r = raspodjela.rasporedi("2026/2027", 2026, redovi, 10, 0.3, false);

        assertEquals(2, r.listaCekanja().size());
        assertEquals(0, primljenih(r.listaCekanja()));
    }

    @Test
    @DisplayName("Naknadni dokaz o uslovu ne istiskuje one koji su ga priložili uz prijavu")
    void naknadniDokazIza() {
        List<RangRedDTO> redovi = List.of(
                red(1, KategorijaRanga.SA_USLOVOM, 90, null, true),    // dokaz naknadno
                red(2, KategorijaRanga.SA_USLOVOM, 80, null, false));  // dokaz uz prijavu

        RangListaAdminDTO r = raspodjela.rasporedi("2026/2027", 2026, redovi, 10, 0.3, false);

        assertEquals(2, r.saUslovom().get(0).idPrijava());
        assertEquals(1, r.saUslovom().get(1).idPrijava());
    }

    @Test
    @DisplayName("Kod jednakih bodova prednost ima ranija prijava")
    void jednakiBodovi() {
        List<RangRedDTO> redovi = List.of(brucos(7, 60), brucos(3, 60));

        RangListaAdminDTO r = raspodjela.rasporedi("2026/2027", 2026, redovi, 10, 0.3, false);

        assertEquals(3, r.brucosi().get(0).idPrijava());
        assertEquals(7, r.brucosi().get(1).idPrijava());
    }

    // ---------- pomoćne ----------

    private static RangRedDTO brucos(int id, double bodovi) {
        return red(id, KategorijaRanga.BRUCOS, bodovi, null, false);
    }

    private static RangRedDTO sUslovom(int id, double bodovi) {
        return red(id, KategorijaRanga.SA_USLOVOM, bodovi, null, false);
    }

    /** Isti redoslijed polja kao u RangListaService.uRed(). */
    private static RangRedDTO red(int id, KategorijaRanga k, double ukupno, String greska, boolean naknadno) {
        return new RangRedDTO(
                0, id, id, "S" + id, "Prezime", "Otac", "Ime",
                k == KategorijaRanga.BRUCOS ? 1 : 3,
                k,
                ukupno, 0, 0, 0, ukupno,
                false,
                naknadno ? "01.10.2026 10:00" : null,
                naknadno,
                greska);
    }

    private static long primljenih(List<RangRedDTO> lista) {
        return lista.stream().filter(RangRedDTO::primljen).count();
    }
}