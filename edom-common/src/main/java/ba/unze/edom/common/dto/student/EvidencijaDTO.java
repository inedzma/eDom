package ba.unze.edom.common.dto.student;

import java.util.List;

public record EvidencijaDTO(Integer godina, List<EvidencijaRedDTO> redovi) {}