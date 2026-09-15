package ba.unze.edom.server.dto;

public record StavkaBodovanja(
        String naziv,
        double bodovi,
        String obrazlozenje,
        boolean verifikovan
) {}