package ba.unze.edom.server.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "uloga")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class Uloga {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_uloga")
    private Integer idUloga;

    @Column(name = "naziv", length = 50)
    private String naziv;
}