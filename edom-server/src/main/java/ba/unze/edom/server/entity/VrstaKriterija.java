package ba.unze.edom.server.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Entity
@Table(name = "vrsta_kriterija")
@Getter @Setter @NoArgsConstructor
public class VrstaKriterija {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_vrsta_kriterija")
    private Integer idVrstaKriterija;

    @Column(name = "sifra", length = 50, nullable = false, unique = true)
    private String sifra;

    @Column(name = "naziv", length = 200, nullable = false)
    private String naziv;

    /** Fiksni bodovi. NULL znaci da se racunaju iz parametra. */
    @Column(name = "bodovi", precision = 6, scale = 2)
    private BigDecimal bodovi;

    @Column(name = "trazi_parametar", nullable = false)
    private boolean traziParametar;

    @Column(name = "naziv_parametra", length = 100)
    private String nazivParametra;

    /** BRANIOCI - uzima se najveci. DODATNI - zbrajaju se. */
    @Column(name = "grupa", length = 50, nullable = false)
    private String grupa;

    @Column(name = "aktivan", nullable = false)
    private boolean aktivan = true;
}