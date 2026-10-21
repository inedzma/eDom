package ba.unze.edom.server.repository;

import ba.unze.edom.server.entity.ZahtjevPromjeneSobe;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ZahtjevPromjeneSobeRepository extends JpaRepository<ZahtjevPromjeneSobe, Integer> {

    List<ZahtjevPromjeneSobe> findByPrijava_IdPrijavaOrderByDatumPodnosenjaDesc(Integer idPrijave);

    boolean existsByPrijava_IdPrijavaAndStatus(Integer idPrijave, String status);

    @Query("""
           select z from ZahtjevPromjeneSobe z
             join fetch z.prijava p
             join fetch p.student s
           where p.akademskaGodina = :godina
           order by z.datumPodnosenja desc
           """)
    List<ZahtjevPromjeneSobe> zaGodinu(@Param("godina") Integer godina);
}