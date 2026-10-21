package api;

import ba.unze.edom.common.dto.DokazUslovaDTO;
import ba.unze.edom.common.dto.RangListaAdminDTO;

import java.util.Map;

public final class RangListaApi {

    private RangListaApi() {}

    public static RangListaAdminDTO dohvati(Integer godina, boolean popuniBrucosima) {
        StringBuilder p = new StringBuilder("/api/rang-lista?popuniBrucosima=").append(popuniBrucosima);
        if (godina != null) p.append("&godina=").append(godina);
        return ApiClient.get(p.toString(), RangListaAdminDTO.class);
    }

    public static DokazUslovaDTO evidentirajDokaz(int idPrijave) {
        return ApiClient.post("/api/rang-lista/prijave/" + idPrijave + "/dokaz-uslova", Map.of(), DokazUslovaDTO.class);
    }

    public static DokazUslovaDTO ponistiDokaz(int idPrijave) {
        return ApiClient.post("/api/rang-lista/prijave/" + idPrijave + "/dokaz-uslova/ponisti", Map.of(), DokazUslovaDTO.class);
    }
}