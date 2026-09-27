package ba.unze.edom.server.service;

import ba.unze.edom.server.dto.RangListaDTO;
import ba.unze.edom.server.dto.RangStavkaDTO;
import ba.unze.edom.server.entity.Prijava;
import ba.unze.edom.server.entity.Student;
import ba.unze.edom.server.repository.PrijavaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RangListaService {

    private static final String STATUS_ODOBRENO = "odobreno";

    private final PrijavaRepository prijavaRepository;

    /** Rang lista za posljednju akademsku godinu za koju postoje prijave. */
    @Transactional(readOnly = true)
    public RangListaDTO trenutna() {
        Integer godina = prijavaRepository.zadnjaAkademskaGodina();
        if (godina == null) {
            return new RangListaDTO(null, List.of(), List.of());
        }
        return zaGodinu(godina);
    }

    @Transactional(readOnly = true)
    public RangListaDTO zaGodinu(Integer godina) {
        List<RangStavkaDTO> brucosi = new ArrayList<>();
        List<RangStavkaDTO> visegodisnji = new ArrayList<>();

        for (Prijava p : prijavaRepository.rangLista(godina)) {
            if (!jeOdobrena(p)) continue;

            RangStavkaDTO stavka = uStavku(p);
            if (jeBrucos(p)) {
                brucosi.add(stavka);
            } else {
                visegodisnji.add(stavka);
            }
        }

        // najviše bodova prvo; kod istih bodova abecedno po prezimenu
        Comparator<RangStavkaDTO> poredak = Comparator
                .comparing(RangStavkaDTO::bodovi, Comparator.reverseOrder())
                .thenComparing(RangStavkaDTO::prezime, Comparator.nullsLast(String::compareToIgnoreCase));
        brucosi.sort(poredak);
        visegodisnji.sort(poredak);

        return new RangListaDTO(godina + "/" + (godina + 1), brucosi, visegodisnji);
    }

    // ---------- pomoćne metode ----------

    private boolean jeOdobrena(Prijava p) {
        return p.getStatus() != null
                && STATUS_ODOBRENO.equalsIgnoreCase(p.getStatus().getNaziv());
    }

    /** Godina iz prijave ima prednost; ako je nema, uzima se godina iz profila studenta. */
    private boolean jeBrucos(Prijava p) {
        Integer god = p.getGodinaStudija() != null
                ? p.getGodinaStudija()
                : p.getStudent().getGodinaStudija();
        return god != null && god == 1;
    }

    private RangStavkaDTO uStavku(Prijava p) {
        Student s = p.getStudent();
        return new RangStavkaDTO(
                s.getIdStudent(),
                s.getPrezime(),
                s.getImeRoditelja(),
                s.getIme(),
                p.getUkupniBodovi() != null ? p.getUkupniBodovi() : 0.0
        );
    }
}