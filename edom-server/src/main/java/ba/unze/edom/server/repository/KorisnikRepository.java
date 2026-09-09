package ba.unze.edom.server.repository;

import ba.unze.edom.server.entity.Korisnik;
import org.hibernate.internal.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface KorisnikRepository extends JpaRepository<Korisnik, Integer> {
    Optional<Korisnik> findByUsername(String username);
    Optional<Korisnik> findByEmail(String email);
}
