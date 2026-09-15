package ba.unze.edom.server.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

@Entity
@Table(name = "prijava_kriterij")
@Getter @Setter @NoArgsConstructor
public class PrijavaKriterij {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_prijava_kriterij")
    private Integer idPrijavaKriterij;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "prijavaid_prijava")
    private Prijava prijava;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "vrsta_kriterijaid")
    private VrstaKriterija vrsta;

    @Column(name = "parametar", precision = 10, scale = 2)
    private BigDecimal parametar;

    /** Bez verifikacije administratora kriterij ne donosi bodove. */
    @Column(name = "verifikovan", nullable = false)
    private boolean verifikovan = false;
}