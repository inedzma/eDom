package ba.unze.edom.server.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/** Želja studenta s weba. Samo se uzima u obzir, nije dodjela. */
@Entity
@Table(name = "zelja_smjestaja")
@Getter
@Setter
public class ZeljaSmjestaja {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_zelja")
    private Integer idZelja;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "prijava_id")
    private Prijava prijava;

    /** 2 = dvokrevetna, 3 = trokrevetna, null = svejedno. */
    @Column(name = "zeljeni_kapacitet")
    private Integer zeljeniKapacitet;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "soba_id")
    private Soba soba;

    /** Broj indeksa ili email cimera. */
    @Column(name = "cimer1", length = 100)
    private String cimer1;

    @Column(name = "cimer2", length = 100)
    private String cimer2;

    @Column(name = "napomena")
    private String napomena;
}