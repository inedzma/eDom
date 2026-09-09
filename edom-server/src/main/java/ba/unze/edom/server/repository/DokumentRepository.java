package ba.unze.edom.server.repository;

import ba.unze.edom.server.entity.Dokument;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DokumentRepository extends JpaRepository<Dokument, Integer> {
    List<Dokument> findByPrijava_IdPrijava(Integer idPrijave);
}