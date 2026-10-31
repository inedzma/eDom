package ba.unze.edom.server.service.prijava;

import ba.unze.edom.common.dto.bodovanje.RezultatBodovanja;

public record StatusPromijenjenEvent(
        String email,
        String ime,
        int akademskaGodina,
        String status,
        String napomena,
        RezultatBodovanja rezultat
) {}