package ba.unze.edom.server.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

/** Ko je u kojoj sobi. Jedna prijava ima najviše jedan smještaj koji nije ISELJEN. */
@Entity
@Table(name = "smjestaj")
@Getter
@Setter
public class Smjestaj {

    public static final String PREDLOZEN  = "PREDLOZEN";   // iz prijedloga, admin još nije potvrdio
    public static final String DODIJELJEN = "DODIJELJEN";  // potvrđeno, student još nije uselio
    public static final String USELJEN    = "USELJEN";
    public static final String ISELJEN    = "ISELJEN";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_smjestaj")
    private Integer idSmjestaj;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "soba_id")
    private Soba soba;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "prijava_id")
    private Prijava prijava;

    @Column(name = "status", nullable = false, length = 20)
    private String status;

    @Column(name = "datum_useljenja")
    private LocalDate datumUseljenja;

    @Column(name = "datum_iseljenja")
    private LocalDate datumIseljenja;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "oznaka_id")
    private OznakaStanara oznaka;

    @Column(name = "kljucevi_preuzeti", nullable = false)
    private int kljuceviPreuzeti;

    @Column(name = "kljucevi_vraceni", nullable = false)
    private int kljuceviVraceni;

    @Column(name = "razlog")
    private String razlog;

    @Column(name = "napomena")
    private String napomena;

    /** Kartica za ishranu: kad je preuzeta / razdužena (null = nije). */
    @Column(name = "kartica_preuzeta")
    private LocalDate karticaPreuzeta;

    @Column(name = "kartica_razduzena")
    private LocalDate karticaRazduzena;

    /** Ljekarsko uvjerenje "sposoban za kolektivni smještaj" predano pri useljenju. */
    @Column(name = "ljekarsko_predano")
    private LocalDate ljekarskoPredano;

    /** Uvjerenje s fakulteta predano pri useljenju (ako nije priloženo uz prijavu). */
    @Column(name = "uvjerenje_fakultet_predano")
    private LocalDate uvjerenjeFakultetPredano;
}