package ba.unze.edom.server.api;

import ba.unze.edom.server.dto.StudentListaDTO;
import ba.unze.edom.server.service.StudentService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/studenti")
@RequiredArgsConstructor
public class StudentApiController {

    private final StudentService studentService;

    @GetMapping
    public List<StudentListaDTO> sviStudenti() {
        return studentService.listaZaAdmina();
    }
}