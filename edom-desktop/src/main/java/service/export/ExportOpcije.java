package service.export;

import ba.unze.edom.common.dto.KategorijaRanga;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

public record ExportOpcije(
        String format,                // "PDF" ili "EXCEL"
        Set<KategorijaRanga> liste,   // svaka odabrana lista ide posebno
        boolean objedinjeno,          // + jedna tabela/list sa svima, po bodovima
        List<RangKolona> kolone,      // odabrane kolone, redom
        boolean konacna,              // samo PDF
        boolean samoPrimljeni,        // važi za brucoše i listu s uslovom
        LocalDate useljenjeOd,        // samo PDF
        LocalDate useljenjeDo         // samo PDF
) {
    public boolean excel() {
        return "EXCEL".equals(format);
    }
}