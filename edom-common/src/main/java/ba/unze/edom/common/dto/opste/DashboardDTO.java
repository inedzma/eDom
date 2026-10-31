package ba.unze.edom.common.dto.opste;

import java.util.Map;

public record DashboardDTO(
        Integer akademskaGodina,
        long brojStudenata,
        int brojPrijava,
        Map<String, Long> poStatusu,   // naziv statusa -> broj prijava
        int cekaProvjeru               // prijave kojima dio bodova čeka verifikaciju
) {}