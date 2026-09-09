package ba.unze.edom.server.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

@Entity
@Table(name = "dokument")
@Getter @Setter @NoArgsConstructor
public class Dokument {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_dokument")
    private Integer idDokument;

    @Column(name = "naziv", length = 100)
    private String naziv;

    @Column(name = "datum_upload")
    private LocalDate datumUpload;

    @Column(name = "broj_bodova", nullable = false)
    private double brojBodova;

    @Column(name = "isdostavljen")
    private Boolean dostavljen;

    @Column(name = "dokumentb64", columnDefinition = "LONGTEXT")
    private String dokumentB64;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "prijavaid_prijava")
    private Prijava prijava;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vrsta_dokumentaid_vrsta")
    private VrstaDokumenta vrstaDokumenta;
}