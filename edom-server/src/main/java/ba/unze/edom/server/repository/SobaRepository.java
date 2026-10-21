package ba.unze.edom.server.repository;

import ba.unze.edom.server.entity.Soba;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface SobaRepository extends JpaRepository<Soba, Integer> {

    /** Sve sobe sa spratom i kategorijom, po spratu pa po broju. */
    @Query("""
           select s from Soba s
             join fetch s.sprat sp
             join fetch s.kategorija k
           order by sp.redoslijed, s.broj
           """)
    List<Soba> sveSaSpratom();

    java.util.Optional<Soba> findByBroj(Integer broj);
}