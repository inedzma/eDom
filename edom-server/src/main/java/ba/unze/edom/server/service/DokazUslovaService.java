package ba.unze.edom.server.service;

import ba.unze.edom.common.dto.DokazUslovaDTO;
import ba.unze.edom.server.entity.Prijava;
import ba.unze.edom.server.repository.PrijavaRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

@Service
public class DokazUslovaService {

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm");
    private static final ZoneId ZONA = ZoneId.of("Europe/Sarajevo");

    private final PrijavaRepository prijavaRepository;

    public DokazUslovaService(PrijavaRepository prijavaRepository) {
        this.prijavaRepository = prijavaRepository;
    }

    /** Admin evidentira da je student naknadno donio dokaz o uslovu za upis. */
    @Transactional
    public DokazUslovaDTO evidentiraj(Integer idPrijave, String admin) {
        Prijava p = nadji(idPrijave);
        if (p.getDokazUslovaAt() != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Dokaz je već evidentiran " + p.getDokazUslovaAt().format(FMT) + ".");
        }
        p.setDokazUslovaAt(LocalDateTime.now(ZONA));
        p.setDokazUslovaEvidentirao(admin);
        return new DokazUslovaDTO(p.getIdPrijava(), p.getDokazUslovaAt().format(FMT));
    }

    /** Samo za grešku admina; dokaz priložen uz prijavu se ovdje ne dira. */
    @Transactional
    public DokazUslovaDTO ponisti(Integer idPrijave) {
        Prijava p = nadji(idPrijave);
        if (p.getDokazUslovaEvidentirao() == null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Može se poništiti samo naknadno evidentiran dokaz.");
        }
        p.setDokazUslovaAt(null);
        p.setDokazUslovaEvidentirao(null);
        return new DokazUslovaDTO(p.getIdPrijava(), null);
    }

    private Prijava nadji(Integer id) {
        return prijavaRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Prijava ne postoji."));
    }
}