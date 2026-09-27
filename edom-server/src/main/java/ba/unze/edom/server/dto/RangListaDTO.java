package ba.unze.edom.server.dto;

import java.util.List;

public record RangListaDTO(
        String akademskaGodina,          // npr. "2025/2026"
        List<RangStavkaDTO> brucosi,
        List<RangStavkaDTO> visegodisnji
) {}