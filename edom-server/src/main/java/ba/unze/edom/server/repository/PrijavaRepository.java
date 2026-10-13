package ba.unze.edom.server.repository;

import ba.unze.edom.server.entity.Prijava;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

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

    @Query("""
           select p from Prijava p
             left join fetch p.status
           where p.student.idStudent = :idStudenta
           order by p.akademskaGodina desc
           """)
    List<Prijava> nadjiZaStudenta(@Param("idStudenta") Integer idStudenta);

    @Query("select max(p.akademskaGodina) from Prijava p")
    Integer zadnjaAkademskaGodina();

    long countByStatus_Naziv(String naziv);

    Optional<Prijava> findByStudent_IdStudentAndAkademskaGodina(
            Integer idStudenta, Integer akademskaGodina);

    @Query("""
           select p from Prijava p
             join fetch p.student s
             left join fetch p.status st
           where p.akademskaGodina = :godina
             and (st is null or st.naziv <> :izostavi)
           order by s.prezime, s.ime
           """)
    List<Prijava> zaAdminListu(@Param("godina") Integer godina,
                               @Param("izostavi") String izostavi);

    @Query("""
           select p from Prijava p
             join fetch p.student
             left join fetch p.status
             left join fetch p.kriteriji
           where p.idPrijava = :id
           """)
    Optional<Prijava> zaDetalje(@Param("id") Integer id);
}