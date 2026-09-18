package ba.unze.edom.server.repository;

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
}