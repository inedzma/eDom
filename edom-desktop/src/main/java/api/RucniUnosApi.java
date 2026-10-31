package api;

import ba.unze.edom.common.dto.bodovanje.RezultatBodovanja;
import ba.unze.edom.common.dto.opste.SifarniciDTO;
import ba.unze.edom.common.dto.prijava.RucniUnosDTO;
import ba.unze.edom.common.dto.prijava.RucniUnosRezultatDTO;
import ba.unze.edom.common.dto.prijava.SpisakUnosDTO;
import ba.unze.edom.common.dto.prijava.SpisakUnosZahtjevDTO;
import ba.unze.edom.common.dto.student.StudentPronadjenDTO;

import java.util.Optional;

public final class RucniUnosApi {

    private RucniUnosApi() {}

    public static SifarniciDTO sifarnici() {
        return ApiClient.get("/api/sifarnici", SifarniciDTO.class);
    }

    public static SpisakUnosDTO spisakDokumenata(SpisakUnosZahtjevDTO z) {
        return ApiClient.post("/api/prijave/rucni-unos/spisak-dokumenata", z, SpisakUnosDTO.class);
    }

    public static RezultatBodovanja pregledBodova(RucniUnosDTO z) {
        return ApiClient.post("/api/prijave/rucni-unos/pregled-bodova", z, RezultatBodovanja.class);
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