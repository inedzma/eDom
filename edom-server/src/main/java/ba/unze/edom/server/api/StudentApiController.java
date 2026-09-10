package ba.unze.edom.server.api;

import ba.unze.edom.server.entity.Student;
import ba.unze.edom.server.repository.StudentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/studenti")
@RequiredArgsConstructor
public class StudentApiController {

    private final StudentRepository repo;

    @GetMapping
    public List<Student> sviStudenti() {
        return repo.findAll();
    }
}