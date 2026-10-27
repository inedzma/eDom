package ba.unze.edom.server.service;

import ba.unze.edom.common.dto.DokumentPapirDTO;
import ba.unze.edom.server.entity.Dokument;
import ba.unze.edom.server.entity.Prijava;
import ba.unze.edom.server.entity.VrstaDokumenta;
import ba.unze.edom.server.exception.PrijavaException;
import ba.unze.edom.server.repository.DokumentRepository;
import ba.unze.edom.server.repository.PrijavaRepository;
import ba.unze.edom.server.repository.VrstaDokumentaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

/** Admin označava dostavljene dokumente i dodaje papirne (bez skena). */
@Service
@RequiredArgsConstructor
public class AdminDokumentiService {

    private final DokumentRepository dokumentRepository;
    private final PrijavaRepository prijavaRepository;
    private final VrstaDokumentaRepository vrstaDokumentaRepository;

    @Transactional
    public void dostavljen(Integer idPrijave, Integer idDokumenta, boolean dostavljen) {
        dokument(idPrijave, idDokumenta).setDostavljen(dostavljen);
    }

    @Transactional
    public void dodajPapir(Integer idPrijave, DokumentPapirDTO z) {
        Prijava p = prijavaRepository.findById(idPrijave)
                .orElseThrow(() -> new PrijavaException("Prijava ne postoji."));
        VrstaDokumenta v = vrstaDokumentaRepository.findById(z.idVrste())
                .orElseThrow(() -> new PrijavaException("Nepoznata vrsta dokumenta."));

        Dokument d = new Dokument();
        d.setNaziv(z.naziv() != null && !z.naziv().isBlank() ? z.naziv().trim() : v.getNaziv());
        d.setVrstaDokumenta(v);
        d.setDatumUpload(LocalDate.now());
        d.setDostavljen(true);
        d.setBrojBodova(0);
        d.setPrijava(p);
        dokumentRepository.save(d);
    }

    /** Briše se samo papirni dokument (bez skena) — za ispravku greške. */
    @Transactional
    public void obrisiPapir(Integer idPrijave, Integer idDokumenta) {
        Dokument d = dokument(idPrijave, idDokumenta);
        if (d.getDokumentB64() != null) {
            throw new PrijavaException("Dokument koji je student priložio online ne briše se ovdje.");
        }
        dokumentRepository.delete(d);
    }

    private Dokument dokument(Integer idPrijave, Integer idDokumenta) {
        Dokument d = dokumentRepository.findById(idDokumenta)
                .orElseThrow(() -> new PrijavaException("Dokument ne postoji."));
        if (d.getPrijava() == null || !d.getPrijava().getIdPrijava().equals(idPrijave)) {
            throw new PrijavaException("Dokument ne pripada ovoj prijavi.");
        }
        return d;
    }
}