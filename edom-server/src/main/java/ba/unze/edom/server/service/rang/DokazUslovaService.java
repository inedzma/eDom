package ba.unze.edom.server.service.rang;

import ba.unze.edom.common.dto.rang.DokazUslovaDTO;
import ba.unze.edom.common.dto.prijava.PrijavaDetaljiDTO;
import ba.unze.edom.common.dto.prijava.PromjenaStatusaDTO;
import ba.unze.edom.server.entity.Prijava;
import ba.unze.edom.server.repository.PrijavaRepository;
import ba.unze.edom.server.service.prijava.PrijavaAdminService;
import ba.unze.edom.server.service.prijava.Statusi;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

/** "Dokaz ✓" i "Poništi dokaz" s rang liste = promjena statusa na čekanju ↔ odobreno. */
@Service
@RequiredArgsConstructor
public class DokazUslovaService {

    private final PrijavaRepository prijavaRepository;
    private final PrijavaAdminService prijavaAdminService;

    @Transactional
    public DokazUslovaDTO evidentiraj(Integer idPrijave) {
        Prijava p = nadji(idPrijave);
        if (!Statusi.NA_CEKANJU.equals(status(p))) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Dokaz se evidentira samo za prijave sa statusom 'na čekanju'.");
        }
        PrijavaDetaljiDTO d = prijavaAdminService.promijeniStatus(idPrijave,
                new PromjenaStatusaDTO(Statusi.ODOBRENO, p.getNapomenaAdmina()));
        return new DokazUslovaDTO(idPrijave, d.uslovUpisa());
    }

    @Transactional
    public DokazUslovaDTO ponisti(Integer idPrijave) {
        Prijava p = nadji(idPrijave);
        if (!Statusi.ODOBRENO.equals(status(p)) || p.getDokazUslovaEvidentirao() == null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Može se poništiti samo naknadno evidentiran dokaz.");
        }
        PrijavaDetaljiDTO d = prijavaAdminService.promijeniStatus(idPrijave,
                new PromjenaStatusaDTO(Statusi.NA_CEKANJU, p.getNapomenaAdmina()));
        return new DokazUslovaDTO(idPrijave, d.uslovUpisa());
    }

    private Prijava nadji(Integer id) {
        return prijavaRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Prijava ne postoji."));
    }

    private String status(Prijava p) {
        return p.getStatus() != null ? p.getStatus().getNaziv() : null;
    }
}