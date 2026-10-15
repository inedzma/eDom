package ba.unze.edom.server.service;

public record PrijavaEvidentiranaEvent(
        String email,
        String ime,
        int akademskaGodina,
        boolean noviNalog
) {}