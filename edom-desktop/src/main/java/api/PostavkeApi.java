package api;

import ba.unze.edom.common.dto.bodovanje.KriterijPostavkaDTO;
import ba.unze.edom.common.dto.bodovanje.KriterijiPostavkeDTO;
import ba.unze.edom.common.dto.opste.PostavkeDTO;

public final class PostavkeApi {

    private PostavkeApi() {}

    public static PostavkeDTO ucitaj() {
        return ApiClient.get("/api/postavke", PostavkeDTO.class);
    }

    public static PostavkeDTO sacuvaj(PostavkeDTO p) {
        return ApiClient.put("/api/postavke", p, PostavkeDTO.class);
    }

    public static KriterijiPostavkeDTO kriteriji() {
        return ApiClient.get("/api/postavke/kriteriji", KriterijiPostavkeDTO.class);
    }

    public static KriterijiPostavkeDTO sacuvajKriterij(KriterijPostavkaDTO k) {
        return ApiClient.put("/api/postavke/kriteriji", k, KriterijiPostavkeDTO.class);
    }

    public static KriterijiPostavkeDTO obrisiKriterij(int id) {
        return ApiClient.post("/api/postavke/kriteriji/" + id + "/obrisi", java.util.Map.of(), KriterijiPostavkeDTO.class);
    }
}