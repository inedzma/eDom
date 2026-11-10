package ba.unze.edom.server.repository;

import ba.unze.edom.server.entity.Student;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface StudentRepository extends JpaRepository<Student, Integer> {
    Optional<Student> findByJmbg(String jmbg);
    List<Student> findByPrezimeContainingIgnoreCase(String dio);

    @Query("select s from Student s left join fetch s.socijalniStatus order by s.prezime, s.ime")
    List<Student> findAllSaStatusom();
}