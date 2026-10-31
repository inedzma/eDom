package api;

import ba.unze.edom.common.dto.opste.DashboardDTO;

public final class DashboardApi {

    private DashboardApi() {}

    public static DashboardDTO dohvati() {
        return ApiClient.get("/api/dashboard", DashboardDTO.class);
    }
}