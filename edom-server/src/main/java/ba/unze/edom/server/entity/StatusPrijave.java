package ba.unze.edom.server.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "status_prijave")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class StatusPrijave {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_status")
    private Integer idStatus;

    @Column(name = "naziv", length = 100)
    private String naziv;
}