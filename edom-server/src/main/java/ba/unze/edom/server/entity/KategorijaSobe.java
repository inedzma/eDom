package ba.unze.edom.server.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/** Boja broja sobe u tabeli: studentska, za goste... */
@Entity
@Table(name = "kategorija_sobe")
@Getter
@Setter
public class KategorijaSobe {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_kategorija")
    private Integer idKategorija;

    @Column(name = "sifra", nullable = false, length = 30)
    private String sifra;

    @Column(name = "naziv", nullable = false, length = 60)
    private String naziv;

    @Column(name = "boja", length = 7)
    private String boja;

    /** false = soba ne ulazi u raspored ni u kapacitet (npr. sobe za goste). */
    @Column(name = "za_studente", nullable = false)
    private boolean zaStudente;

    @Column(name = "opis")
    private String opis;
}