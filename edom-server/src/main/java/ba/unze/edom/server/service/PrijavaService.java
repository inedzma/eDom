package ba.unze.edom.server.service;

import ba.unze.edom.server.dto.PrijavaPregledDTO;
import ba.unze.edom.server.repository.PrijavaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PrijavaService {

    private final PrijavaRepository prijavaRepository;

    @Transactional(readOnly = true)
    public List<PrijavaPregledDTO> pregledZaStudenta(Integer idStudenta) {
        return prijavaRepository.nadjiZaStudenta(idStudenta).stream()
                .map(p -> new PrijavaPregledDTO(
                        p.getIdPrijava(),
                        p.getAkademskaGodina(),
                        p.getDatumPrijave(),
                        p.getStatus() != null ? p.getStatus().getNaziv() : "Nepoznat",
                        p.getUkupniBodovi(),
                        p.getDokumenti() != null ? p.getDokumenti().size() : 0
                ))
                .toList();
    }
}