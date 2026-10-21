package api;

import ba.unze.edom.common.dto.*;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/** Pozivi servera za sobe i raspored. */
public final class SobeApi {

    private SobeApi() {}

    // ---------- pregled ----------

    public static SobePregledDTO pregled(int godina) {
        return ApiClient.get("/api/sobe?godina=" + godina, SobePregledDTO.class);
    }

    public static List<StudentBezSobeDTO> bezSobe(int godina) {
        return Arrays.asList(ApiClient.get("/api/sobe/bez-sobe?godina=" + godina, StudentBezSobeDTO[].class));
    }

    public static List<OznakaStanaraDTO> oznake() {
        return Arrays.asList(ApiClient.get("/api/sobe/oznake", OznakaStanaraDTO[].class));
    }

    // ---------- raspored ----------

    public static PrijedlogRasporedaDTO napraviPrijedlog(int godina) {
        return ApiClient.post("/api/sobe/raspored/prijedlog?godina=" + godina, Map.of(), PrijedlogRasporedaDTO.class);
    }

    public static int potvrdiPrijedlog(int godina) {
        return broj(ApiClient.post("/api/sobe/raspored/potvrdi?godina=" + godina, Map.of(), Map.class), "broj");
    }

    public static int ponistiPrijedlog(int godina) {
        return broj(ApiClient.post("/api/sobe/raspored/ponisti?godina=" + godina, Map.of(), Map.class), "broj");
    }

    // ---------- ručne izmjene ----------

    public static void dodijeli(int idPrijava, int idSoba) {
        ApiClient.post("/api/sobe/dodijeli", new DodjelaSobeDTO(idPrijava, idSoba), Map.class);
    }

    public static void useli(int idSmjestaj, int kljucevi) {
        ApiClient.post("/api/sobe/smjestaj/" + idSmjestaj + "/useli?kljucevi=" + kljucevi, Map.of(), Map.class);
    }

    /** Vraća koliko ključeva nije vraćeno. */
    public static int iseli(int idSmjestaj, int vraceno) {
        return broj(ApiClient.post("/api/sobe/smjestaj/" + idSmjestaj + "/iseli?vraceno=" + vraceno,
                Map.of(), Map.class), "nedostaje");
    }

    public static void ukloni(int idSmjestaj) {
        ApiClient.post("/api/sobe/smjestaj/" + idSmjestaj + "/ukloni", Map.of(), Map.class);
    }

    public static void postaviOznaku(int idSmjestaj, Integer idOznake) {
        ApiClient.post("/api/sobe/smjestaj/" + idSmjestaj + "/oznaka"
                + (idOznake != null ? "?idOznake=" + idOznake : ""), Map.of(), Map.class);
    }

    public static void postaviAktivna(int idSoba, boolean aktivna, String napomena) {
        String q = "?aktivna=" + aktivna
                + (napomena != null ? "&napomena=" + URLEncoder.encode(napomena, StandardCharsets.UTF_8) : "");
        ApiClient.post("/api/sobe/" + idSoba + "/aktivna" + q, Map.of(), Map.class);
    }

    private static int broj(Map<?, ?> m, String kljuc) {
        Object v = m != null ? m.get(kljuc) : null;
        return v instanceof Number n ? n.intValue() : 0;
    }

    public static List<ZahtjevSobeDTO> zahtjevi(int godina) {
        return Arrays.asList(ApiClient.get("/api/sobe/zahtjevi?godina=" + godina, ZahtjevSobeDTO[].class));
    }

    public static void odgovoriNaZahtjev(int id, String status, String odgovor) {
        String q = "?status=" + status + (odgovor != null && !odgovor.isBlank()
                ? "&odgovor=" + URLEncoder.encode(odgovor, StandardCharsets.UTF_8) : "");
        ApiClient.post("/api/sobe/zahtjevi/" + id + "/odgovor" + q, Map.of(), Map.class);
    }

    public static StudentDetaljiDTO studentDetalji(int idPrijava) {
        return ApiClient.get("/api/sobe/student/" + idPrijava, StudentDetaljiDTO.class);
    }

    /** Vraća true ako je mail poslan. */
    public static boolean posaljiNapomenu(int idPrijava, String tekst) {
        Map<?, ?> r = ApiClient.post("/api/sobe/student/" + idPrijava + "/napomena",
                new NovaNapomenaDTO(tekst), Map.class);
        return r != null && Boolean.TRUE.equals(r.get("poslano"));
    }
}