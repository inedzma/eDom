package ba.unze.edom.server.api;

import ba.unze.edom.common.dto.student.StudentListaDTO;
import ba.unze.edom.common.dto.student.StudentPronadjenDTO;
import ba.unze.edom.server.service.prijava.RucniUnosService;
import ba.unze.edom.server.service.student.StudentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
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

    private final RucniUnosService rucniUnosService;

    @GetMapping("/jmbg/{jmbg}")
    public ResponseEntity<StudentPronadjenDTO> poJmbg(@PathVariable String jmbg) {
        return rucniUnosService.nadjiPoJmbg(jmbg)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }
}