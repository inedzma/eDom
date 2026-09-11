package ba.unze.edom.server.repository;

import ba.unze.edom.server.entity.Korisnik;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface KorisnikRepository extends JpaRepository<Korisnik, Integer> {
    Optional<Korisnik> findByUsername(String username);
    Optional<Korisnik> findByStudent_IdStudent(Integer idStudenta);

    Optional<Korisnik> findByEmail(String email);
}