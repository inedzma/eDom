package ba.unze.edom.server.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
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

    @Column(name = "prosjek", precision = 4, scale = 2)
    private BigDecimal prosjek;

    @Column(name = "godina_studija")
    private Integer godinaStudija;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "socijalni_status_id_status")
    private SocijalniStatus socijalniStatus;

    @Column(name = "udaljenost_km", precision = 6, scale = 2)
    private BigDecimal udaljenostKm;

    @Column(name = "broj_clanova_domacinstva")
    private Integer brojClanovaDomacinstva;

    @Column(name = "ukupna_primanja", precision = 10, scale = 2)
    private BigDecimal ukupnaPrimanja;

    @Column(name = "polozeni_ispiti")
    private Integer polozeniIspiti;

    @OneToMany(mappedBy = "prijava", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PrijavaKriterij> kriteriji = new ArrayList<>();

    @Column(name = "uspjeh_verifikovan", nullable = false)
    private boolean uspjehVerifikovan = false;

    @Column(name = "primanja_verifikovana", nullable = false)
    private boolean primanjaVerifikovana = false;

    @Column(name = "udaljenost_verifikovana", nullable = false)
    private boolean udaljenostVerifikovana = false;

    @Column(name = "napomena_administratora", length = 500)
    private String napomenaAdmina;

    /** Pitanje 1: koji put student upisuje tekuću godinu (1 = prvi put). */
    @Column(name = "upis_godine_put")
    private Integer upisGodinePut;

    /** Pitanje 7: da li je ikada obnavljao godinu. */
    @Column(name = "obnavljao_godinu")
    private Boolean obnavljaoGodinu;

    /** Pitanje 7: koje akademske godine je obnavljao. */
    @Column(name = "obnovljene_godine", length = 100)
    private String obnovljeneGodine;

    public void dodajKriterij(PrijavaKriterij k) {
        kriteriji.add(k);
        k.setPrijava(this);
    }

    // pomocne metode za odrzavanje obje strane veze
    public void dodajDokument(Dokument d) {
        dokumenti.add(d);
        d.setPrijava(this);
    }

    public void ukloniDokument(Dokument d) {
        dokumenti.remove(d);
        d.setPrijava(null);
    }

    /** Kad je dostavljen dokaz o ispunjavanju uslova za upis naredne godine; null = nije dostavljen. */
    @Column(name = "dokaz_uslova_at")
    private LocalDateTime dokazUslovaAt;

    /** Email admina koji je dokaz naknadno evidentirao; null = dokaz priložen uz prijavu. */
    @Column(name = "dokaz_uslova_evidentirao", length = 100)
    private String dokazUslovaEvidentirao;

    public LocalDateTime getDokazUslovaAt() {
        return dokazUslovaAt;
    }

    public void setDokazUslovaAt(LocalDateTime dokazUslovaAt) {
        this.dokazUslovaAt = dokazUslovaAt;
    }

    public String getDokazUslovaEvidentirao() {
        return dokazUslovaEvidentirao;
    }

    public void setDokazUslovaEvidentirao(String dokazUslovaEvidentirao) {
        this.dokazUslovaEvidentirao = dokazUslovaEvidentirao;
    }
}