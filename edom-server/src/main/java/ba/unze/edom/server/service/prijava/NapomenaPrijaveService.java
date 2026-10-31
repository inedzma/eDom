package ba.unze.edom.server.service.prijava;

import ba.unze.edom.server.entity.Prijava;
import ba.unze.edom.server.exception.PrijavaException;
import ba.unze.edom.server.repository.PrijavaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Napomena uz prijavu: piše je student pri predaji, admin je može ispraviti. */
@Service
@RequiredArgsConstructor
public class NapomenaPrijaveService {

    public static final int MAX = 500;

    private final PrijavaRepository prijavaRepository;

    @Transactional
    public void sacuvaj(Integer idPrijave, String napomena) {
        Prijava p = prijavaRepository.findById(idPrijave)
                .orElseThrow(() -> new PrijavaException("Prijava ne postoji."));
        p.setNapomena(ocisti(napomena));
    }

    /** Prazno → null; predugo → greška. */
    public static String ocisti(String napomena) {
        if (napomena == null) return null;
        String t = napomena.trim();
        if (t.isEmpty()) return null;
        if (t.length() > MAX) {
            throw new PrijavaException("Napomena može imati najviše " + MAX + " znakova.");
        }
        return t;
    }
}