package ba.unze.edom.server.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/**
 * Zahtjev studenta za promjenu sobe (poslije dodjele).
 * Sistem ništa ne mijenja sam: administrator odobri ili odbije, a premješta ručno.
 */
@Entity
@Table(name = "zahtjev_promjene_sobe")
@Getter
@Setter
public class ZahtjevPromjeneSobe {

    public static final String ZAMJENA = "ZAMJENA";
    public static final String PREMJESTAJ = "PREMJESTAJ";

    public static final String NA_CEKANJU = "NA_CEKANJU";
    public static final String ODOBREN = "ODOBREN";
    public static final String ODBIJEN = "ODBIJEN";
    public static final String POVUCEN = "POVUCEN";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_zahtjev")
    private Integer idZahtjev;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "prijava_id")
    private Prijava prijava;

    @Column(name = "vrsta", nullable = false, length = 20)
    private String vrsta;

    @Column(name = "sa_studentom", length = 100)
    private String saStudentom;

    @Column(name = "zeljena_soba")
    private Integer zeljenaSoba;

    @Column(name = "razlog", nullable = false, length = 500)
    private String razlog;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "datum_podnosenja", nullable = false)
    private LocalDateTime datumPodnosenja;

    @Column(name = "datum_odgovora")
    private LocalDateTime datumOdgovora;

    @Column(name = "odgovor", length = 255)
    private String odgovor;
}