package ba.unze.edom.server.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

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