module ba.unze.edom.common {
    requires static jakarta.validation;

    exports ba.unze.edom.common.dto;
    opens ba.unze.edom.common.dto;

    exports ba.unze.edom.common.util;
    opens ba.unze.edom.common.util;
}