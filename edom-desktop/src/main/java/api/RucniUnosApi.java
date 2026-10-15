package api;

import ba.unze.edom.common.dto.RucniUnosDTO;
import ba.unze.edom.common.dto.RucniUnosRezultatDTO;
import ba.unze.edom.common.dto.SifarniciDTO;
import ba.unze.edom.common.dto.StudentPronadjenDTO;

import java.util.Optional;

public final class RucniUnosApi {

    private RucniUnosApi() {}

    public static SifarniciDTO sifarnici() {
        return ApiClient.get("/api/sifarnici", SifarniciDTO.class);
    }

    public static Optional<StudentPronadjenDTO> nadjiPoJmbg(String jmbg) {
        try {
            return Optional.of(ApiClient.get("/api/studenti/jmbg/" + jmbg, StudentPronadjenDTO.class));
        } catch (ApiException e) {
            if (e.getStatus() == 404) return Optional.empty();
            throw e;
        }
    }

    public static RucniUnosRezultatDTO posalji(RucniUnosDTO zahtjev) {
        return ApiClient.post("/api/prijave/rucni-unos", zahtjev, RucniUnosRezultatDTO.class);
    }
}