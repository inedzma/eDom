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

    /** false = soba za goste: ne ulazi u raspored studenata ni u kapacitet. */
    @Column(name = "za_studente", nullable = false)
    private boolean zaStudente = true;

    /**
     * Ručno zadan spol sobe (npr. soba rezervisana samo za studentice).
     * Obično je null: spol tada određuju stanari, a prazna soba prima bilo koga.
     */
    @Column(name = "spol", length = 1)
    private String spol;

    @Column(name = "broj_kljuceva", nullable = false)
    private Integer brojKljuceva;

    /** false = van upotrebe (kvar, renoviranje). */
    @Column(name = "aktivna", nullable = false)
    private boolean aktivna;

    @Column(name = "napomena")
    private String napomena;

    /** Ručno zadan spol sobe ili null. */
    public String spolSobe() {
        return spol != null && !spol.isBlank() ? spol : null;
    }

    /** Ulazi li soba u raspored studenata. */
    public boolean zaRaspored() {
        return aktivna && zaStudente;
    }
}