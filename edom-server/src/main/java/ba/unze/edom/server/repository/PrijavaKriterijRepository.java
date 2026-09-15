package ba.unze.edom.server.repository;

import ba.unze.edom.server.entity.PrijavaKriterij;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PrijavaKriterijRepository extends JpaRepository<PrijavaKriterij, Integer> {
    List<PrijavaKriterij> findByPrijava_IdPrijava(Integer idPrijave);
}