package ba.unze.edom.server.service.sobe;

import ba.unze.edom.common.dto.sobe.ZahtjevSobeDTO;
import ba.unze.edom.server.entity.*;
import ba.unze.edom.server.exception.PrijavaException;
import ba.unze.edom.server.repository.SmjestajRepository;
import ba.unze.edom.server.repository.ZahtjevPromjeneSobeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

/** Zahtjevi za promjenu sobe na desktopu: admin samo odobri/odbije i upiše odgovor. */
@Service
@RequiredArgsConstructor
public class ZahtjeviSobaService {

    private final ZahtjevPromjeneSobeRepository zahtjevRepository;
    private final SmjestajRepository smjestajRepository;

    @Transactional(readOnly = true)
    public List<ZahtjevSobeDTO> zaGodinu(int godina) {
        List<ZahtjevPromjeneSobe> svi = zahtjevRepository.zaGodinu(godina);

        // trenutne sobe i studenti po broju indeksa / emailu, samo za prikaz
        Map<Integer, Integer> sobaPoPrijavi = new HashMap<>();
        Map<String, Prijava> prijavaPoOznaci = new HashMap<>();
        for (Smjestaj sm : smjestajRepository.aktivniZaGodinu(godina)) {
            Prijava p = sm.getPrijava();
            sobaPoPrijavi.put(p.getIdPrijava(), sm.getSoba().getBroj());
            Student s = p.getStudent();
            if (s.getBrojIndeksa() != null) prijavaPoOznaci.put(kljuc(s.getBrojIndeksa()), p);
            if (s.getEmail() != null) prijavaPoOznaci.put(kljuc(s.getEmail()), p);
        }

        List<ZahtjevSobeDTO> lista = new ArrayList<>();
        for (ZahtjevPromjeneSobe z : svi) {
            Prijava p = z.getPrijava();
            Student s = p.getStudent();

            String drugiIme = null;
            Integer drugiSoba = null;
            Boolean drugiPodnio = null;
            if (ZahtjevPromjeneSobe.ZAMJENA.equals(z.getVrsta()) && z.getSaStudentom() != null) {
                Prijava drugi = prijavaPoOznaci.get(kljuc(z.getSaStudentom()));
                if (drugi != null) {
                    drugiIme = drugi.getStudent().getIme() + " " + drugi.getStudent().getPrezime();
                    drugiSoba = sobaPoPrijavi.get(drugi.getIdPrijava());
                    drugiPodnio = svi.stream().anyMatch(x ->
                            x.getPrijava().getIdPrijava().equals(drugi.getIdPrijava())
                                    && ZahtjevPromjeneSobe.ZAMJENA.equals(x.getVrsta())
                                    && ZahtjevPromjeneSobe.NA_CEKANJU.equals(x.getStatus())
                                    && x.getSaStudentom() != null
                                    && (kljuc(x.getSaStudentom()).equals(kljuc(s.getBrojIndeksa()))
                                    || kljuc(x.getSaStudentom()).equals(kljuc(s.getEmail()))));
                }
            }

            lista.add(new ZahtjevSobeDTO(
                    z.getIdZahtjev(), p.getIdPrijava(), s.getIme() + " " + s.getPrezime(), s.getBrojIndeksa(),
                    sobaPoPrijavi.get(p.getIdPrijava()),
                    z.getVrsta(), z.getSaStudentom(), drugiIme, drugiSoba, z.getZeljenaSoba(),
                    z.getRazlog(), z.getStatus(), z.getDatumPodnosenja(), z.getOdgovor(), drugiPodnio));
        }
        return lista;
    }

    @Transactional
    public void odgovori(Integer id, String status, String odgovor) {
        if (!ZahtjevPromjeneSobe.ODOBREN.equals(status) && !ZahtjevPromjeneSobe.ODBIJEN.equals(status)) {
            throw new PrijavaException("Status mora biti ODOBREN ili ODBIJEN.");
        }
        ZahtjevPromjeneSobe z = zahtjevRepository.findById(id)
                .orElseThrow(() -> new PrijavaException("Zahtjev ne postoji."));
        if (!ZahtjevPromjeneSobe.NA_CEKANJU.equals(z.getStatus())) {
            throw new PrijavaException("Zahtjev je već riješen.");
        }
        z.setStatus(status);
        z.setOdgovor(odgovor == null || odgovor.isBlank() ? null : odgovor.trim());
        z.setDatumOdgovora(LocalDateTime.now());
    }

    private static String kljuc(String s) {
        return s == null ? "" : s.trim().toLowerCase();
    }
}