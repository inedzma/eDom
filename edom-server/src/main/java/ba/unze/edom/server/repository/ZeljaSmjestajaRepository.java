package ba.unze.edom.server.repository;

import ba.unze.edom.server.entity.ZeljaSmjestaja;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ZeljaSmjestajaRepository extends JpaRepository<ZeljaSmjestaja, Integer> {

    @Query("""
           select z from ZeljaSmjestaja z
             join fetch z.prijava p
             left join fetch z.soba s
           where p.akademskaGodina = :godina
           """)
    List<ZeljaSmjestaja> zaGodinu(@Param("godina") Integer godina);
}