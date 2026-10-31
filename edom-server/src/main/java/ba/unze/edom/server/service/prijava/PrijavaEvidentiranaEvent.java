package ba.unze.edom.server.service.prijava;

public record PrijavaEvidentiranaEvent(
        String email,
        String ime,
        int akademskaGodina,
        boolean noviNalog,
        String sifra
) {}