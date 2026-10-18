package ba.unze.edom.server.repository;

import ba.unze.edom.server.entity.VrstaDokumenta;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface VrstaDokumentaRepository extends JpaRepository<VrstaDokumenta, Integer> {
    List<VrstaDokumenta> findBySifraIsNotNull();
}
