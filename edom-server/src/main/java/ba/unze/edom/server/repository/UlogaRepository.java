package ba.unze.edom.server.repository;

import ba.unze.edom.server.entity.Uloga;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UlogaRepository extends JpaRepository<Uloga, Integer> {
    Optional<Uloga> findByNaziv(String naziv);
}
