package ba.unze.edom.common.dto;

public record StavkaBodovanja(
        String naziv,
        double bodovi,
        String obrazlozenje,
        boolean verifikovan
) {}