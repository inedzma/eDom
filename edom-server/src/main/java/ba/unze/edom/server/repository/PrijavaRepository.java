package ba.unze.edom.server.repository;

import ba.unze.edom.server.entity.Prijava;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface PrijavaRepository extends JpaRepository<Prijava, Integer> {

    List<Prijava> findByAkademskaGodina(Integer godina);

    List<Prijava> findByStudent_IdStudent(Integer idStudenta);

    @Query("""
           select p from Prijava p
             join fetch p.student s
             left join fetch p.status
           where p.akademskaGodina = :godina
           order by p.ukupniBodovi desc
           """)
    List<Prijava> rangLista(Integer godina);

    long countByStatus_Naziv(String naziv);
}