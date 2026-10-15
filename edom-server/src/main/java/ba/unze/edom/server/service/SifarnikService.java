package ba.unze.edom.server.service;

import ba.unze.edom.common.dto.*;
import ba.unze.edom.server.repository.SocijalniStatusRepository;
import ba.unze.edom.server.repository.VrstaDokumentaRepository;
import ba.unze.edom.server.repository.VrstaKriterijaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SifarnikService {

    private final SocijalniStatusRepository socijalniStatusRepository;
    private final VrstaKriterijaRepository vrstaKriterijaRepository;
    private final VrstaDokumentaRepository vrstaDokumentaRepository;

    @Transactional(readOnly = true)
    public SifarniciDTO svi() {
        return new SifarniciDTO(
                socijalniStatusRepository.findAll(Sort.by("naziv")).stream()
                        .map(s -> new SocijalniStatusDTO(s.getIdStatus(), s.getNaziv()))
                        .toList(),
                vrstaKriterijaRepository.findByAktivanTrueOrderByGrupaAscNazivAsc().stream()
                        .map(v -> new VrstaKriterijaDTO(v.getIdVrstaKriterija(), v.getSifra(), v.getNaziv(),
                                v.getGrupa(), v.isTraziParametar(), v.getNazivParametra(),
                                v.getDokaz() != null ? v.getDokaz().getIdVrsta() : null,
                                v.getDokaz() != null ? v.getDokaz().getNaziv() : null))
                        .toList(),
                vrstaDokumentaRepository.findAll(Sort.by("kategorija", "naziv")).stream()
                        .map(v -> new VrstaDokumentaDTO(v.getIdVrsta(), v.getNaziv(), v.getKategorija()))
                        .toList()
        );
    }
}