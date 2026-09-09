package ba.unze.edom.server.service;

import ba.unze.edom.server.entity.*;
import org.springframework.stereotype.Service;

@Service
public class BodovanjeService {

    /**
     * Zbir bodova svih DOSTAVLJENIH dokumenata prijave.
     * Vraca double - stara implementacija je vracala int i odsijecala decimale.
     */
    public double izracunajUkupneBodove(Prijava prijava) {
        if (prijava == null || prijava.getDokumenti() == null) {
            return 0.0;
        }
        return prijava.getDokumenti().stream()
                .filter(d -> Boolean.TRUE.equals(d.getDostavljen()))
                .mapToDouble(Dokument::getBrojBodova)
                .sum();
    }
}