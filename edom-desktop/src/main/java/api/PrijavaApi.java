package api;

import ba.unze.edom.common.dto.DokumentSadrzajDTO;
import ba.unze.edom.common.dto.PrijavaDetaljiDTO;
import ba.unze.edom.common.dto.PrijavaListaDTO;
import com.fasterxml.jackson.core.type.TypeReference;

import java.util.List;

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
}