package api;

import ba.unze.edom.common.dto.opste.ObavijestDTO;
import com.fasterxml.jackson.core.type.TypeReference;

import java.util.List;

public final class ObavijestiApi {

    private ObavijestiApi() {}

    public static List<ObavijestDTO> sve() {
        return ApiClient.get("/api/obavijesti", new TypeReference<List<ObavijestDTO>>() {});
    }
}