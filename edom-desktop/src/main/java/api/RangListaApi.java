package api;

import ba.unze.edom.common.dto.RangListaAdminDTO;

public final class RangListaApi {

    private RangListaApi() {}

    public static RangListaAdminDTO dohvati(Integer godina) {
        String putanja = godina == null ? "/api/rang-lista" : "/api/rang-lista?godina=" + godina;
        return ApiClient.get(putanja, RangListaAdminDTO.class);
    }
}