package ba.unze.edom.server.repository;

import ba.unze.edom.server.entity.StatusPrijave;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface StatusPrijaveRepository extends JpaRepository<StatusPrijave, Integer> {
    Optional<StatusPrijave> findByNaziv(String naziv);
}