package ba.unze.edom.server.service.prijava;

/** Student je na webu podnio prijavu (u izradi → na pregledu). */
public record PrijavaPodnesenaEvent(
        String email,
        String ime,
        int akademskaGodina,
        String sifra
) {}