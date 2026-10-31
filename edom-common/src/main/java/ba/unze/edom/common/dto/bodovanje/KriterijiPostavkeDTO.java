package ba.unze.edom.common.dto.bodovanje;

import java.util.List;

public record KriterijiPostavkeDTO(List<KriterijPostavkaDTO> kriteriji, List<Dokument> dokumenti) {

    public record Dokument(Integer id, String naziv) {
        @Override
        public String toString() {
            return naziv;
        }
    }
}