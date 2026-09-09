package ba.unze.edom.server.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Entity
@Table(name = "student")
@Getter @Setter @NoArgsConstructor
public class Student {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_student")
    private Integer idStudent;

    @Column(name = "ime", length = 50)
    private String ime;

    @Column(name = "prezime", length = 50)
    private String prezime;

    @Column(name = "broj_indeksa", length = 20, unique = true)
    private String brojIndeksa;

    @Column(name = "fakultet", length = 100)
    private String fakultet;

    @Column(name = "godina_studija")
    private Integer godinaStudija;

    // decimal(4,2) u bazi -> BigDecimal, ne double
    @Column(name = "prosjek", precision = 4, scale = 2)
    private BigDecimal prosjek;

    @Column(name = "email", length = 100)
    private String email;

    @Column(name = "telefon", length = 100)
    private String telefon;

    @Column(name = "adresa", length = 50, nullable = false)
    private String adresa;

    @Column(name = "ime_roditelja", length = 20, nullable = false)
    private String imeRoditelja;

    @Column(name = "jmbg", length = 13, nullable = false)
    private String jmbg;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "socijalni_status_id_status")
    private SocijalniStatus socijalniStatus;
}