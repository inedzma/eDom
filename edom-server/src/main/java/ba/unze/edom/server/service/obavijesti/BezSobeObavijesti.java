package ba.unze.edom.server.service.obavijesti;

import ba.unze.edom.common.dto.opste.ObavijestDTO;
import ba.unze.edom.common.dto.rang.RangListaAdminDTO;
import ba.unze.edom.common.dto.rang.RangRedDTO;
import ba.unze.edom.server.entity.Smjestaj;
import ba.unze.edom.server.service.rang.RangListaService;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/** Primljeni na rang listi (iznad crte), a nemaju dodijeljenu ni useljenu sobu. */
@Component
@Order(3)
@RequiredArgsConstructor
public class BezSobeObavijesti implements IzvorObavijesti {

    @PersistenceContext
    private EntityManager em;

    private final RangListaService rangListaService;

    @Override
    public List<ObavijestDTO> obavijesti() {
        RangListaAdminDTO l = rangListaService.zaAdmina(null, null, false);

        Set<Integer> primljene = Stream.of(l.brucosi(), l.saUslovom())
                .flatMap(List::stream)
                .filter(RangRedDTO::primljen)
                .map(RangRedDTO::idPrijava)
                .collect(Collectors.toSet());
        if (primljene.isEmpty()) return List.of();

        Set<Integer> saSobom = new HashSet<>(em.createQuery(
                        "SELECT s.prijava.idPrijava FROM Smjestaj s WHERE s.status IN :statusi", Integer.class)
                .setParameter("statusi", List.of(Smjestaj.DODIJELJEN, Smjestaj.USELJEN))
                .getResultList());

        long bez = primljene.stream().filter(id -> !saSobom.contains(id)).count();
        if (bez == 0) return List.of();

        return List.of(new ObavijestDTO(
                "BEZ_SOBE",
                "BEZ_SOBE:" + bez,   // kad se broj promijeni, obavijest je opet "nova"
                bez + (bez == 1 ? " primljeni student nema sobu" : " primljenih studenata nema sobu"),
                "Primljeni su na rang listi, a soba im nije dodijeljena (predložene sobe se ne računaju dok ih ne potvrdite).",
                null,
                null,
                null));
    }
}