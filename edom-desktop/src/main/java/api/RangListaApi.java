package api;

import ba.unze.edom.common.dto.rang.DokazUslovaDTO;
import ba.unze.edom.common.dto.rang.ObjavaRangListeDTO;
import ba.unze.edom.common.dto.rang.RangListaAdminDTO;
import ba.unze.edom.common.dto.rang.RangObjavaDTO;

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

    public static RangObjavaDTO objavi(String vrsta, boolean popuniBrucosima,
                                       java.time.LocalDate useljenjeOd, java.time.LocalDate useljenjeDo) {
        return ApiClient.post("/api/rang-lista/objavi",
                new ObjavaRangListeDTO(vrsta, popuniBrucosima, useljenjeOd, useljenjeDo), RangObjavaDTO.class);
    }
    public static RangObjavaDTO zadnjaObjava() {
        return ApiClient.get("/api/rang-lista/objava", RangObjavaDTO.class);
    }
}