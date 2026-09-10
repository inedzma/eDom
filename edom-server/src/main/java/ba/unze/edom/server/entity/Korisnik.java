package ba.unze.edom.server.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Entity
@Table(name = "korisnik")
@Getter @Setter @NoArgsConstructor
public class Korisnik {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id_korisnik")
    private Integer idKorisnik;

    @Column(name = "ime", length = 50)
    private String ime;

    @Column(name = "prezime", length = 50)
    private String prezime;

    @Column(name = "username", length = 50)
    private String username;

    @Column(name = "password_hash", length = 100)
    private String passwordHash;

    @Column(name = "email", length = 50)
    private String email;

    @Column(name = "zadnja_prijava")
    private Instant zadnjaPrijava;

    @Column(name = "reset_token_hash", length = 255)
    private String resetToken;

    @Column(name = "reset_token_istek")
    private Instant resetTokenIstek;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "studentid_student")
    private Student student;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "ulogaid_uloga")
    private Uloga uloga;
}