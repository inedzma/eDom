package ba.unze.edom.server.service;

import ba.unze.edom.common.dto.RezultatBodovanja;

public record StatusPromijenjenEvent(
        String email,
        String ime,
        int akademskaGodina,
        String status,
        String napomena,
        RezultatBodovanja rezultat
) {}