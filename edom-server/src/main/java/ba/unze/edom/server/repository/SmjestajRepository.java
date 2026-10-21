package ba.unze.edom.server.repository;

import ba.unze.edom.server.entity.Prijava;
import ba.unze.edom.server.entity.Smjestaj;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface SmjestajRepository extends JpaRepository<Smjestaj, Integer> {

    /** Svi smještaji godine koji nisu iseljeni, sa studentom, sobom i oznakom. */
    @Query("""
           select sm from Smjestaj sm
             join fetch sm.prijava p
             join fetch p.student st
             join fetch sm.soba so
             left join fetch sm.oznaka o
           where p.akademskaGodina = :godina
             and sm.status <> 'ISELJEN'
           """)
    List<Smjestaj> aktivniZaGodinu(@Param("godina") Integer godina);

    /** Aktivni smještaj jedne prijave (najviše jedan). */
    @Query("""
           select sm from Smjestaj sm
           where sm.prijava.idPrijava = :idPrijave
             and sm.status <> 'ISELJEN'
           """)
    Optional<Smjestaj> aktivniZaPrijavu(@Param("idPrijave") Integer idPrijave);

    @Query("""
           select count(sm) from Smjestaj sm
           where sm.soba.idSoba = :idSobe
             and sm.prijava.akademskaGodina = :godina
             and sm.status <> 'ISELJEN'
           """)
    long zauzetoUSobi(@Param("idSobe") Integer idSobe, @Param("godina") Integer godina);

    /** Brisanje nepotvrđenog prijedloga prije novog. */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
           delete from Smjestaj sm
           where sm.status = 'PREDLOZEN'
             and sm.prijava.idPrijava in (
                 select p.idPrijava from Prijava p where p.akademskaGodina = :godina)
           """)
    int obrisiPrijedlog(@Param("godina") Integer godina);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
           update Smjestaj sm set sm.status = 'DODIJELJEN'
           where sm.status = 'PREDLOZEN'
             and sm.prijava.idPrijava in (
                 select p.idPrijava from Prijava p where p.akademskaGodina = :godina)
           """)
    int potvrdiPrijedlog(@Param("godina") Integer godina);

    /** Prijave koje ulaze u raspored: odobrene (i zaključene) za godinu, najbolji prvi. */
    @Query("""
           select p from Prijava p
             join fetch p.student st
             join fetch p.status s
           where p.akademskaGodina = :godina
             and s.naziv in :statusi
           order by p.ukupniBodovi desc, p.idPrijava asc
           """)
    List<Prijava> zaRaspored(@Param("godina") Integer godina,
                             @Param("statusi") Collection<String> statusi);

    /** Stanari jedne sobe u godini (bez iseljenih). */
    @Query("""
           select sm from Smjestaj sm
             join fetch sm.prijava p
             join fetch p.student st
           where sm.soba.idSoba = :idSobe
             and p.akademskaGodina = :godina
             and sm.status <> 'ISELJEN'
           """)
    List<Smjestaj> stanariSobe(@Param("idSobe") Integer idSobe, @Param("godina") Integer godina);

    /** Prijave studenta, najnovija prva (za stranicu "Moj smještaj"). */
    @Query("""
           select p from Prijava p
             join fetch p.status s
           where p.student.idStudent = :idStudenta
           order by p.akademskaGodina desc, p.idPrijava desc
           """)
    List<Prijava> prijaveStudenta(@Param("idStudenta") Integer idStudenta);
}