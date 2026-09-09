package ba.unze.edom.server.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "prijava")
@Getter @Setter @NoArgsConstructor
public class Prijava {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_prijava")
    private Integer idPrijava;

    @Column(name = "datum_prijave")
    private LocalDate datumPrijave;

    @Column(name = "ukupni_bodovi")
    private Double ukupniBodovi;

    @Column(name = "napomena", length = 500)
    private String napomena;

    @Column(name = "akademska_godina")
    private Integer akademskaGodina;

    // naslijedjeno ime kolone iz stare seme
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "studentid_student2")
    private Student student;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "status_prijaveid_status")
    private StatusPrijave status;

    @OneToMany(mappedBy = "prijava", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Dokument> dokumenti = new ArrayList<>();

    // pomocne metode za odrzavanje obje strane veze
    public void dodajDokument(Dokument d) {
        dokumenti.add(d);
        d.setPrijava(this);
    }

    public void ukloniDokument(Dokument d) {
        dokumenti.remove(d);
        d.setPrijava(null);
    }
}