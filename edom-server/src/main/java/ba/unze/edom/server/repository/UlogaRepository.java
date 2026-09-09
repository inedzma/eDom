package ba.unze.edom.server.repository;

import ba.unze.edom.server.entity.Uloga;
import org.hibernate.internal.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UlogaRepository extends JpaRepository<Uloga, Integer> {
    Optional<Uloga> findByNaziv(String naziv);
}
