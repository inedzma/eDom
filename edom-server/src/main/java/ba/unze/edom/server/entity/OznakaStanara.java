package ba.unze.edom.server.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

/** Boja imena stanara u tabeli (značenje se upisuje u naziv). */
@Entity
@Table(name = "oznaka_stanara")
@Getter
@Setter
public class OznakaStanara {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_oznaka")
    private Integer idOznaka;

    @Column(name = "naziv", nullable = false, length = 60)
    private String naziv;

    @Column(name = "boja", nullable = false, length = 7)
    private String boja;

    @Column(name = "opis")
    private String opis;
}