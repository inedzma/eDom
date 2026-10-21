package service.export;

import ba.unze.edom.common.dto.KategorijaRanga;

import java.time.LocalDate;
import java.util.List;
import java.util.Set;

public record ExportOpcije(
        Set<KategorijaRanga> liste,   // svaka odabrana lista ide kao posebna tabela
        boolean objedinjeno,          // + jedna tabela sa svima, po bodovima
        boolean javna,                // true = samo R.br, šifra, bodovi
        List<RangKolona> kolone,      // za internu verziju: kolone vidljive u tabeli
        boolean konacna,
        boolean samoPrimljeni,        // važi za brucoše i listu s uslovom
        LocalDate useljenjeOd,
        LocalDate useljenjeDo
) {}