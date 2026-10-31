package ba.unze.edom.common.dto.rang;

import java.util.List;

public record RangListaAdminDTO(
        String akademskaGodina,     // "2026/2027"
        Integer godina,             // 2026
        int kapacitet,
        int kvotaBrucosi,
        int kvotaVisi,
        int primljenoBrucosa,
        int primljenoVisih,
        int slobodno,
        boolean popunjenoBrucosima,
        List<RangRedDTO> brucosi,
        List<RangRedDTO> saUslovom,
        List<RangRedDTO> listaCekanja
) {}