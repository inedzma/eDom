package ba.unze.edom.server.repository;

import ba.unze.edom.server.entity.Uplata;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface UplataRepository extends JpaRepository<Uplata, Integer> {

    /** Sve uplate za mjesece od–do (jedna akademska godina). */
    List<Uplata> findByMjesecBetween(LocalDate od, LocalDate doMjeseca);

    Optional<Uplata> findByStudent_IdStudentAndMjesec(Integer idStudenta, LocalDate mjesec);

    /** Sve uplate studenta kroz godine, npr. da se vidi je li ranije redovno plaćao. */
    List<Uplata> findByStudent_IdStudentOrderByMjesecAsc(Integer idStudenta);
}