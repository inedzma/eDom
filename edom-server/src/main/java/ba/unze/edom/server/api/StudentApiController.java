package ba.unze.edom.server.api;

import ba.unze.edom.common.dto.student.StudentDetaljiDTO;
import ba.unze.edom.common.dto.student.StudentIzmjenaDTO;
import ba.unze.edom.common.dto.student.StudentListaDTO;
import ba.unze.edom.common.dto.student.StudentPronadjenDTO;
import ba.unze.edom.server.service.prijava.RucniUnosService;
import ba.unze.edom.server.service.student.StudentDetaljiService;
import ba.unze.edom.server.service.student.StudentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/studenti")
@RequiredArgsConstructor
public class StudentApiController {

    private final StudentService studentService;
    private final RucniUnosService rucniUnosService;
    private final StudentDetaljiService studentDetaljiService;

    @GetMapping
    public List<StudentListaDTO> sviStudenti() {
        return studentService.listaZaAdmina();
    }

    @GetMapping("/jmbg/{jmbg}")
    public ResponseEntity<StudentPronadjenDTO> poJmbg(@PathVariable String jmbg) {
        return rucniUnosService.nadjiPoJmbg(jmbg)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /** Administrator mijenja podatke studenta (otvoren preko prijave). */
    @PutMapping("/prijava/{idPrijave}")
    public StudentDetaljiDTO izmijeni(@PathVariable Integer idPrijave,
                                      @Valid @RequestBody StudentIzmjenaDTO z) {
        return studentDetaljiService.izmijeni(idPrijave, z);
    }
}