package ba.unze.edom.server.service;

import ba.unze.edom.server.exception.RegistracijaException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import ba.unze.edom.common.dto.RezultatBodovanja;
import ba.unze.edom.common.dto.StavkaBodovanja;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${edom.mail.od}")
    private String od;

    @Value("${edom.mail.naziv}")
    private String naziv;

    public void posalji(String kome, String naslov, String tekst) {
        try {
            MimeMessage poruka = mailSender.createMimeMessage();
            MimeMessageHelper h = new MimeMessageHelper(poruka, "UTF-8");
            h.setFrom(new InternetAddress(od, naziv, "UTF-8"));
            h.setTo(kome);
            h.setSubject(naslov);
            h.setText(tekst);
            mailSender.send(poruka);
        } catch (Exception e) {
            log.error("Slanje e-maila na {} nije uspjelo", kome, e);
            throw new RegistracijaException(
                    "Trenutno nije moguće poslati e-mail. Pokušajte kasnije.");
        }
    }

    public void posaljiResetKod(String kome, String kod) {
        posalji(kome, "Reset lozinke - E-Dom",
                "Poštovani,\n\n"
                        + "Vaš kod za reset lozinke je:\n\n"
                        + "    " + kod + "\n\n"
                        + "Kod važi 15 minuta.\n\n"
                        + "Ako niste tražili reset lozinke, ignorišite ovu poruku.\n\n"
                        + "E-Dom Zenica");
    }

    public void posaljiPrijavaOdobrena(String kome, String ime, String prezime,
                                       int akademskaGodina) {
        posalji(kome, "Odobrena prijava za studentski dom",
                "Poštovani/a " + ime + " " + prezime + ",\n\n"
                        + "Obavještavamo Vas da je Vaša prijava za smještaj u "
                        + "Studentski dom Zenica za akademsku godinu "
                        + akademskaGodina + " odobrena.\n\n"
                        + "Srdačan pozdrav,\nStudentski centar Zenica");
    }

    public void posaljiObavijestORucnomUnosu(String email, String ime, int godina,
                                             boolean noviNalog, String webUrl) {

        String akGod = godina + "/" + (godina + 1);

        StringBuilder tekst = new StringBuilder()
                .append("Poštovani/a ").append(ime).append(",\n\n")
                .append("vaša prijava za smještaj u Studentskom centru Univerziteta u Zenici ")
                .append("za akademsku ").append(akGod).append(". godinu evidentirana je u sistemu E-Dom.\n\n");

        if (noviNalog) {
            tekst.append("Za vas je kreiran korisnički nalog. Korisničko ime je ova e-mail adresa.\n")
                    .append("Da biste postavili lozinku, otvorite ")
                    .append(webUrl).append("/zaboravljena-lozinka")
                    .append(", unesite ovu e-mail adresu i slijedite upute.\n\n")
                    .append("Nakon prijave možete pratiti status svoje prijave i rang listu.\n\n");
        } else {
            tekst.append("Status prijave možete pratiti nakon prijave na ").append(webUrl).append(".\n\n");
        }

        tekst.append("Studentski centar Zenica");

        SimpleMailMessage poruka = new SimpleMailMessage();
        poruka.setFrom(od);                 // ili kako se kod tebe zove adresa pošiljaoca
        poruka.setTo(email);
        poruka.setSubject("E-Dom: vaša prijava za smještaj je evidentirana");
        poruka.setText(tekst.toString());
        mailSender.send(poruka);
    }

    public void posaljiPromjenuStatusa(String email, String ime, int godina, String status,
                                       String napomena, RezultatBodovanja r, String webUrl) {

        String akGod = godina + "/" + (godina + 1);
        boolean odobreno = "odobreno".equals(status);

        StringBuilder t = new StringBuilder()
                .append("Poštovani/a ").append(ime).append(",\n\n")
                .append("vaša prijava za smještaj u Studentskom centru Univerziteta u Zenici za akademsku ")
                .append(akGod).append(". godinu je ")
                .append(odobreno ? "ODOBRENA" : "ODBIJENA").append(".\n\n");

        if (r != null && odobreno) {
            t.append("Bodovano:\n");
            r.stavke().stream().filter(StavkaBodovanja::verifikovan).forEach(s ->
                    t.append("  • ").append(s.naziv()).append(": ")
                            .append(String.format("%.2f", s.bodovi()))
                            .append(s.obrazlozenje() != null ? " (" + s.obrazlozenje() + ")" : "")
                            .append("\n"));
            t.append(String.format("Ukupno: %.2f bodova%n%n", r.potvrdjeno()));

            var nebodovano = r.stavke().stream()
                    .filter(s -> !s.verifikovan() && s.bodovi() > 0).toList();
            if (!nebodovano.isEmpty()) {
                t.append("Nije bodovano jer odgovarajući dokument nije dostavljen ili provjeren:\n");
                nebodovano.forEach(s -> t.append("  • ").append(s.naziv())
                        .append(String.format(" (%.2f)", s.bodovi())).append("\n"));
                t.append("\n");
            }
        }

        if (napomena != null && !napomena.isBlank()) {
            t.append(odobreno ? "Napomena: " : "Razlog: ").append(napomena).append("\n\n");
        }

        if (!odobreno) {
            t.append("Ako naknadno dostavite tražene dokumente, obratite se administraciji doma.\n\n");
        }

        t.append("Status prijave i rang listu možete pratiti na ").append(webUrl).append(".\n\n")
                .append("Studentski centar Zenica");

        SimpleMailMessage poruka = new SimpleMailMessage();
        poruka.setFrom(od);
        poruka.setTo(email);
        poruka.setSubject("E-Dom: prijava za smještaj " + (odobreno ? "odobrena" : "odbijena"));
        poruka.setText(t.toString());
        mailSender.send(poruka);
    }
}