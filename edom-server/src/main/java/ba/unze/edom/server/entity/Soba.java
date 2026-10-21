package ba.unze.edom.server.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "soba")
@Getter
@Setter
public class Soba {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_soba")
    private Integer idSoba;

    @Column(name = "broj", nullable = false)
    private Integer broj;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "sprat_id")
    private Sprat sprat;

    @Column(name = "kapacitet", nullable = false)
    private Integer kapacitet;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "kategorija_id")
    private KategorijaSobe kategorija;

    /** Pregazi spol sprata za ovu sobu; null = kao sprat. */
    @Column(name = "spol", length = 1)
    private String spol;

    @Column(name = "broj_kljuceva", nullable = false)
    private Integer brojKljuceva;

    /** false = van upotrebe (kvar, renoviranje). */
    @Column(name = "aktivna", nullable = false)
    private boolean aktivna;

    @Column(name = "napomena")
    private String napomena;

    /** Spol sobe: njen, inače sprata. null = nije određeno. */
    public String spolSobe() {
        if (spol != null && !spol.isBlank()) return spol;
        return sprat != null ? sprat.getSpol() : null;
    }

    /** Ulazi li soba u raspored studenata. */
    public boolean zaRaspored() {
        return aktivna && kategorija != null && kategorija.isZaStudente();
    }
}