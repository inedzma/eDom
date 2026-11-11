package ba.unze.edom.server.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

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

    // naslijeđeno ime kolone iz stare šeme
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "studentid_student2")
    private Student student;

    @Column(name = "akademska_godina")
    private Integer akademskaGodina;

    @Column(name = "datum_prijave")
    private LocalDate datumPrijave;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "status_prijaveid_status")
    private StatusPrijave status;

    @Column(name = "napomena", length = 500)
    private String napomena;

    @Column(name = "napomena_administratora", length = 500)
    private String napomenaAdmina;

    // ---------- podaci za bodovanje (stanje u trenutku konkursa) ----------

    @Column(name = "godina_studija")
    private Integer godinaStudija;

    /** Koji put student upisuje tekuću godinu (1 = prvi put); 2+ donosi kaznene bodove. */
    @Column(name = "upis_godine_put")
    private Integer upisGodinePut;

    @Column(name = "prosjek", precision = 4, scale = 2)
    private BigDecimal prosjek;

    @Column(name = "polozeni_ispiti")
    private Integer polozeniIspiti;

    @Column(name = "udaljenost_km", precision = 6, scale = 2)
    private BigDecimal udaljenostKm;

    @Column(name = "broj_clanova_domacinstva")
    private Integer brojClanovaDomacinstva;

    @Column(name = "ukupna_primanja", precision = 10, scale = 2)
    private BigDecimal ukupnaPrimanja;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "socijalni_status_id_status")
    private SocijalniStatus socijalniStatus;

    /** Potvrđeni bodovi, preračunavaju se pri svakoj izmjeni. */
    @Column(name = "ukupni_bodovi")
    private Double ukupniBodovi;

    // ---------- podaci samo za obrazac prijave ----------

    /** Da li je ikada obnavljao godinu. */
    @Column(name = "obnavljao_godinu")
    private Boolean obnavljaoGodinu;

    /** Koje akademske godine je obnavljao. */
    @Column(name = "obnovljene_godine", length = 100)
    private String obnovljeneGodine;

    // ---------- dokaz o uslovu za upis naredne godine ----------

    /** Kad je dokaz dostavljen; null = nije. */
    @Column(name = "dokaz_uslova_at")
    private LocalDateTime dokazUslovaAt;

    /** Email admina koji ga je naknadno evidentirao; null = priložen uz prijavu. */
    @Column(name = "dokaz_uslova_evidentirao", length = 100)
    private String dokazUslovaEvidentirao;

    // ---------- veze ----------

    @OneToMany(mappedBy = "prijava", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PrijavaKriterij> kriteriji = new ArrayList<>();

    @OneToMany(mappedBy = "prijava", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Dokument> dokumenti = new ArrayList<>();

    /** Održava obje strane veze. */
    public void dodajKriterij(PrijavaKriterij k) {
        kriteriji.add(k);
        k.setPrijava(this);
    }

    public void dodajDokument(Dokument d) {
        dokumenti.add(d);
        d.setPrijava(this);
    }
}