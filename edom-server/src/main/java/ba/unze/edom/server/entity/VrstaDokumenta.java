package ba.unze.edom.server.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "vrsta_dokumenta")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class VrstaDokumenta {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_vrsta")
    private Integer idVrsta;

    @Column(name = "naziv", length = 100)
    private String naziv;

    @Column(name = "kategorija", length = 30, nullable = false)
    private String kategorija = "Ostalo";

    /** Stalna šifra (npr. CIPS, KUCNA_LISTA); kod se oslanja na nju, ne na naziv. */
    @Column(name = "sifra", length = 50, unique = true)
    private String sifra;
}