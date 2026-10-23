package ba.unze.edom.common.dto;

import java.util.List;

public record EvidencijaDTO(Integer godina, List<EvidencijaRedDTO> redovi) {}