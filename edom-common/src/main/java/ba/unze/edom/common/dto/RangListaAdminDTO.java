package ba.unze.edom.common.dto;

import java.util.List;

public record RangListaAdminDTO(
        String akademskaGodina,     // "2026/2027"
        Integer godina,             // 2026
        List<RangRedDTO> brucosi,
        List<RangRedDTO> visegodisnji
) {}