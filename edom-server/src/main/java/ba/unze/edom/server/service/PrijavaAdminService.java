package ba.unze.edom.server.service;

import ba.unze.edom.common.dto.PrijavaDetaljiDTO;
import ba.unze.edom.common.dto.PrijavaListaDTO;
import ba.unze.edom.common.dto.RezultatBodovanja;
import ba.unze.edom.server.entity.Prijava;
import ba.unze.edom.server.exception.NevalidnaPrijavaException;
import ba.unze.edom.server.exception.PrijavaException;
import ba.unze.edom.server.mapper.PrijaveMapper;
import ba.unze.edom.server.repository.DokumentRepository;
import ba.unze.edom.server.repository.PrijavaRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PrijavaAdminService {

    private final PrijavaRepository prijavaRepository;
    private final DokumentRepository dokumentRepository;
    private final BodovanjeService bodovanjeService;

    // ---------- LISTA ----------

    @Transactional(readOnly = true)
    public List<PrijavaListaDTO> lista(Integer godina) {
        Integer g = godina != null ? godina : prijavaRepository.zadnjaAkademskaGodina();
        if (g == null) return List.of();

        Map<Integer, Integer> brojDok = new HashMap<>();
        for (Object[] red : dokumentRepository.brojPoPrijavi(g)) {
            brojDok.put((Integer) red[0], ((Number) red[1]).intValue());
        }

        return prijavaRepository.zaAdminListu(g, Statusi.U_IZRADI).stream()
                .map(p -> uRed(p, brojDok.getOrDefault(p.getIdPrijava(), 0)))
                .toList();
    }

    private PrijavaListaDTO uRed(Prijava p, int brojDok) {
        try {
            return PrijaveMapper.uListu(p, bodovanjeService.izracunaj(p), null, brojDok);
        } catch (NevalidnaPrijavaException e) {
            return PrijaveMapper.uListu(p, null, e.getMessage(), brojDok);
        }
    }

    // ---------- DETALJI ----------

    @Transactional(readOnly = true)
    public PrijavaDetaljiDTO detalji(Integer idPrijave) {
        Prijava p = prijavaRepository.zaDetalje(idPrijave)
                .orElseThrow(() -> new PrijavaException("Prijava ne postoji."));

        RezultatBodovanja r = null;
        String greska = null;
        try {
            r = bodovanjeService.izracunaj(p);
        } catch (NevalidnaPrijavaException e) {
            greska = e.getMessage();
        }

        return PrijaveMapper.uDetalje(p, r, greska,
                dokumentRepository.pregledZaAdmina(idPrijave));
    }

    // ---------- SADRŽAJ DOKUMENTA ----------

    @Transactional(readOnly = true)
    public String sadrzajDokumenta(Integer idPrijave, Integer idDokumenta) {
        if (!dokumentRepository.existsByIdDokumentAndPrijava_IdPrijava(idDokumenta, idPrijave)) {
            throw new PrijavaException("Dokument ne pripada ovoj prijavi.");
        }
        return dokumentRepository.nadjiSadrzaj(idDokumenta)
                .orElseThrow(() -> new PrijavaException("Dokument nema priloženu datoteku."));
    }
}