package api;

import ba.unze.edom.common.dto.PorukaDTO;
import com.fasterxml.jackson.core.type.TypeReference;

import java.util.List;
import java.util.Map;

public final class PorukeApi {

    private PorukeApi() {}

    public static List<PorukaDTO> nepročitane() {
        return ApiClient.get("/api/poruke", new TypeReference<List<PorukaDTO>>() {});
    }

    public static void procitano(String id) {
        ApiClient.post("/api/poruke/" + id + "/procitano", Map.of(), Map.class);
    }
}