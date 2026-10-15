package ba.unze.edom.server.service;

import ba.unze.edom.common.dto.DashboardDTO;
import ba.unze.edom.common.dto.PrijavaListaDTO;
import ba.unze.edom.server.repository.PrijavaRepository;
import ba.unze.edom.server.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final PrijavaAdminService prijavaAdminService;
    private final PrijavaRepository prijavaRepository;
    private final StudentRepository studentRepository;

    /** Brojevi za tekuću akademsku godinu — iz iste liste koju prikazuje ekran Prijave. */
    @Transactional(readOnly = true)
    public DashboardDTO pregled() {
        Integer godina = prijavaRepository.zadnjaAkademskaGodina();
        List<PrijavaListaDTO> prijave = prijavaAdminService.lista(godina);

        Map<String, Long> poStatusu = prijave.stream()
                .collect(Collectors.groupingBy(
                        p -> p.status() != null ? p.status() : "nepoznat",
                        LinkedHashMap::new,
                        Collectors.counting()));

        int cekaProvjeru = (int) prijave.stream().filter(PrijavaListaDTO::cekaVerifikaciju).count();

        return new DashboardDTO(godina, studentRepository.count(),
                prijave.size(), poStatusu, cekaProvjeru);
    }
}