package ba.unze.edom.server.repository;

import ba.unze.edom.server.entity.*;
import org.springframework.data.jpa.repository.*;
import java.util.*;

public interface StudentRepository extends JpaRepository<Student, Integer> {
    Optional<Student> findByBrojIndeksa(String brojIndeksa);
    Optional<Student> findByJmbg(String jmbg);
    List<Student> findByPrezimeContainingIgnoreCase(String dio);
}