package ba.unze.edom.server.repository;

import ba.unze.edom.server.entity.VrstaKriterija;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface VrstaKriterijaRepository extends JpaRepository<VrstaKriterija, Integer> {
    List<VrstaKriterija> findByAktivanTrueOrderByGrupaAscNazivAsc();
    List<VrstaKriterija> findByGrupaAndAktivanTrue(String grupa);
    Optional<VrstaKriterija> findBySifra(String sifra);
}