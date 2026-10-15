package ba.unze.edom.server.api;

import ba.unze.edom.common.dto.DashboardDTO;
import ba.unze.edom.server.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardApiController {

    private final DashboardService dashboardService;

    @GetMapping
    public DashboardDTO pregled() {
        return dashboardService.pregled();
    }
}