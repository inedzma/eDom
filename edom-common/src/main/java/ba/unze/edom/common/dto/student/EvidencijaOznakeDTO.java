package ba.unze.edom.common.dto.student;

/** Kvačice iz detalja studenta; datum upisuje server. */
public record EvidencijaOznakeDTO(
        boolean karticaPreuzeta,
        boolean karticaRazduzena,
        boolean ljekarskoPredano,
        boolean uvjerenjeFakultetPredano
) {}