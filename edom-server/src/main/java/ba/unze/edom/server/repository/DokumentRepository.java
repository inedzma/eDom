package ba.unze.edom.server.repository;

import ba.unze.edom.common.dto.DokumentDTO;
import ba.unze.edom.server.dto.DokumentPregled;
import ba.unze.edom.server.entity.Dokument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface DokumentRepository extends JpaRepository<Dokument, Integer> {

    /** Lista za prikaz — bez Base64 sadrzaja. */
    List<DokumentPregled> findByPrijava_IdPrijavaOrderByIdDokumentAsc(Integer idPrijave);

    /** Sadrzaj se cita SAMO kad se dokument stvarno preuzima. */
    @Query("select d.dokumentB64 from Dokument d where d.idDokument = :id")
    Optional<String> nadjiSadrzaj(@Param("id") Integer id);

    long countByPrijava_IdPrijava(Integer idPrijave);

    /** Lista dokumenata BEZ učitavanja Base64 sadržaja. */
    @Query("""
           select new ba.unze.edom.common.dto.DokumentDTO(
               d.idDokument, d.naziv, v.naziv, d.datumUpload, d.dostavljen,
               case when d.dokumentB64 is not null then true else false end)
           from Dokument d
             left join d.vrstaDokumenta v
           where d.prijava.idPrijava = :idPrijave
           order by d.idDokument
           """)
    List<DokumentDTO> pregledZaAdmina(@Param("idPrijave") Integer idPrijave);

    /** Broj dokumenata po prijavi — samo brojanje, bez učitavanja. */
    @Query("""
           select d.prijava.idPrijava, count(d)
           from Dokument d
           where d.prijava.akademskaGodina = :godina
           group by d.prijava.idPrijava
           """)
    List<Object[]> brojPoPrijavi(@Param("godina") Integer godina);

    boolean existsByIdDokumentAndPrijava_IdPrijava(Integer idDokument, Integer idPrijava);
}