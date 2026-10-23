package api;

import ba.unze.edom.common.dto.EvidencijaDTO;
import ba.unze.edom.common.dto.EvidencijaOznakeDTO;
import ba.unze.edom.common.dto.GenerisaniDokumentDTO;

import java.util.Map;

public final class EvidencijaApi {

    private EvidencijaApi() {}

    public static EvidencijaDTO lista(Integer godina) {
        return ApiClient.get(godina == null ? "/api/evidencija" : "/api/evidencija?godina=" + godina,
                EvidencijaDTO.class);
    }

    public static void sacuvajOznake(int idPrijave, EvidencijaOznakeDTO z) {
        ApiClient.put("/api/evidencija/" + idPrijave + "/oznake", z, Map.class);
    }

    public static GenerisaniDokumentDTO ugovor(int idPrijave) {
        return ApiClient.post("/api/evidencija/" + idPrijave + "/ugovor", Map.of(), GenerisaniDokumentDTO.class);
    }

    public static GenerisaniDokumentDTO revers(int idPrijave) {
        return ApiClient.post("/api/evidencija/" + idPrijave + "/revers", Map.of(), GenerisaniDokumentDTO.class);
    }
}