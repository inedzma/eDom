package ba.unze.edom.server.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

/** Jedan plaćeni mjesec smještaja. Neplaćeni mjeseci se ne čuvaju, nego računaju. */
@Entity
@Table(name = "uplata")
@Getter
@Setter
public class Uplata {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_uplata")
    private Integer idUplata;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "student_id")
    private Student student;

    /** Prvi dan mjeseca za koji se plaća (npr. 2026-10-01 = oktobar 2026). */
    @Column(name = "mjesec", nullable = false)
    private LocalDate mjesec;

    @Column(name = "datum_uplate", nullable = false)
    private LocalDate datumUplate;
}