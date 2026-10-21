package ba.unze.edom.server.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "sprat")
@Getter
@Setter
public class Sprat {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_sprat")
    private Integer idSprat;

    @Column(name = "naziv", nullable = false, length = 40)
    private String naziv;

    @Column(name = "redoslijed", nullable = false)
    private Integer redoslijed;

    /** 'M' / 'Z'; null = određuju stanari. */
    @Column(name = "spol", length = 1)
    private String spol;
}