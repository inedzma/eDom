package api;

import ba.unze.edom.common.dto.*;
import com.fasterxml.jackson.core.type.TypeReference;

import java.util.List;
import java.util.Map;

public final class PrijavaApi {

    private PrijavaApi() {}

    /** godina = null -> server vraća zadnju akademsku godinu */
    public static List<PrijavaListaDTO> lista(Integer godina) {
        String putanja = godina == null ? "/api/prijave" : "/api/prijave?godina=" + godina;
        return ApiClient.get(putanja, new TypeReference<List<PrijavaListaDTO>>() {});
    }

    public static PrijavaDetaljiDTO detalji(int idPrijave) {
        return ApiClient.get("/api/prijave/" + idPrijave, PrijavaDetaljiDTO.class);
    }

    public static String sadrzajDokumenta(int idPrijave, int idDokumenta) {
        return ApiClient.get("/api/prijave/" + idPrijave + "/dokumenti/" + idDokumenta + "/sadrzaj",
                DokumentSadrzajDTO.class).b64();
    }

    public static PrijavaDetaljiDTO verifikuj(int idPrijave, VerifikacijaDTO z) {
        return ApiClient.put("/api/prijave/" + idPrijave + "/verifikacija", z, PrijavaDetaljiDTO.class);
    }

    public static PrijavaDetaljiDTO promijeniStatus(int idPrijave, PromjenaStatusaDTO z) {
        return ApiClient.put("/api/prijave/" + idPrijave + "/status", z, PrijavaDetaljiDTO.class);
    }

    public static BodovanjeDetaljnoDTO bodovanje(int idPrijave) {
        return ApiClient.get("/api/prijave/" + idPrijave + "/bodovanje", BodovanjeDetaljnoDTO.class);
    }

    public static BodovanjeDetaljnoDTO sacuvajBodovanje(int idPrijave, BodovanjeIzmjenaDTO z) {
        return ApiClient.put("/api/prijave/" + idPrijave + "/bodovanje", z, BodovanjeDetaljnoDTO.class);
    }

    public static DokumentiSpisakDTO dokumentiSpisak(int idPrijave) {
        return ApiClient.get("/api/prijave/" + idPrijave + "/dokumenti-spisak", DokumentiSpisakDTO.class);
    }

    public static void dostavljen(int idPrijave, int idDokumenta, boolean dostavljen) {
        ApiClient.put("/api/prijave/" + idPrijave + "/dokumenti/" + idDokumenta + "/dostavljen",
                new DostavljenDTO(dostavljen), Map.class);
    }

    public static void dodajPapir(int idPrijave, int idVrste, String naziv) {
        ApiClient.post("/api/prijave/" + idPrijave + "/dokumenti/papir",
                new DokumentPapirDTO(idVrste, naziv), Map.class);
    }

    public static void obrisiPapir(int idPrijave, int idDokumenta) {
        ApiClient.post("/api/prijave/" + idPrijave + "/dokumenti/" + idDokumenta + "/obrisi", Map.of(), Map.class);
    }

    public static void sacuvajNapomenu(int idPrijave, String napomena) {
        ApiClient.put("/api/prijave/" + idPrijave + "/napomena", new NapomenaPrijaveDTO(napomena), Map.class);
    }
}