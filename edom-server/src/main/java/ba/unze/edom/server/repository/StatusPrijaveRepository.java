package ba.unze.edom.server.repository;

import ba.unze.edom.server.entity.StatusPrijave;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface StatusPrijaveRepository extends JpaRepository<StatusPrijave, Integer> {
    Optional<StatusPrijave> findByNaziv(String naziv);
}