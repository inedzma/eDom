package api;

import ba.unze.edom.common.dto.uplate.PodsjetnikUplateDTO;
import ba.unze.edom.common.dto.uplate.UplateTabelaDTO;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

public final class UplateApi {

    private UplateApi() {}

    public static UplateTabelaDTO tabela(int akademskaGodina) {
        return ApiClient.get("/api/uplate?godina=" + akademskaGodina, UplateTabelaDTO.class);
    }

    /** mjesec u obliku "2026-10" */
    public static void evidentiraj(int idStudenta, String mjesec, LocalDate datum) {
        ApiClient.post("/api/uplate/" + idStudenta + "/" + mjesec + "?datum=" + datum, Map.of(), Map.class);
    }

    public static void ponisti(int idStudenta, String mjesec) {
        ApiClient.post("/api/uplate/" + idStudenta + "/" + mjesec + "/obrisi", Map.of(), Map.class);
    }

    /**
     * Šalje podsjetnik o neplaćenim mjesecima.
     * Odgovor: { "poslano": broj, "nijePoslano": [ "Ime Prezime: razlog", … ] }
     */
    @SuppressWarnings("unchecked")
    public static Map<String, Object> podsjetnik(int akademskaGodina, List<Integer> idStudenti, String napomena) {
        return ApiClient.post("/api/uplate/podsjetnik",
                new PodsjetnikUplateDTO(akademskaGodina, idStudenti, napomena), Map.class);
    }
}