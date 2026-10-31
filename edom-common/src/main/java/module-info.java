module ba.unze.edom.common {
    requires static jakarta.validation;

    exports ba.unze.edom.common.dto;
    opens ba.unze.edom.common.dto;

    exports ba.unze.edom.common.util;
    opens ba.unze.edom.common.util;
    exports ba.unze.edom.common.dto.korisnik;
    opens ba.unze.edom.common.dto.korisnik;
    exports ba.unze.edom.common.dto.student;
    opens ba.unze.edom.common.dto.student;
    exports ba.unze.edom.common.dto.prijava;
    opens ba.unze.edom.common.dto.prijava;
    exports ba.unze.edom.common.dto.dokumenti;
    opens ba.unze.edom.common.dto.dokumenti;
    exports ba.unze.edom.common.dto.bodovanje;
    opens ba.unze.edom.common.dto.bodovanje;
    exports ba.unze.edom.common.dto.rang;
    opens ba.unze.edom.common.dto.rang;
    exports ba.unze.edom.common.dto.sobe;
    opens ba.unze.edom.common.dto.sobe;
    exports ba.unze.edom.common.dto.opste;
    opens ba.unze.edom.common.dto.opste;
}