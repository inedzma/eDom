package ba.unze.edom.server.service.obavijesti;

import ba.unze.edom.server.exception.RegistracijaException;
import ba.unze.edom.server.service.prijava.Statusi;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import ba.unze.edom.common.dto.bodovanje.RezultatBodovanja;
import ba.unze.edom.common.dto.bodovanje.StavkaBodovanja;

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
                                             boolean noviNalog, String sifra, String webUrl) {

        String akGod = godina + "/" + (godina + 1);

        StringBuilder tekst = new StringBuilder()
                .append("Poštovani/a ").append(ime).append(",\n\n")
                .append("vaša prijava za smještaj u Studentskom centru Univerziteta u Zenici ")
                .append("za akademsku ").append(akGod).append(". godinu evidentirana je u sistemu E-Dom.\n\n")
                .append(sifraTekst(sifra, webUrl));

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
        poruka.setFrom(od);
        poruka.setTo(email);
        poruka.setSubject("E-Dom: vaša prijava za smještaj je evidentirana");
        poruka.setText(tekst.toString());
        mailSender.send(poruka);
    }

    /** Potvrda nakon što student sam podnese prijavu na webu. */
    public void posaljiPotvrduPrijave(String email, String ime, int godina, String sifra, String webUrl) {

        String akGod = godina + "/" + (godina + 1);

        String tekst = "Poštovani/a " + ime + ",\n\n"
                + "vaša prijava za smještaj u Studentskom centru Univerziteta u Zenici za akademsku "
                + akGod + ". godinu je zaprimljena i čeka pregled dokumentacije.\n\n"
                + sifraTekst(sifra, webUrl)
                + "O svakoj promjeni statusa prijave bićete obaviješteni e-mailom. "
                + "Status možete pratiti i na " + webUrl + ".\n\n"
                + "Studentski centar Zenica";

        posalji(email, "E-Dom: prijava za smještaj je zaprimljena", tekst);
    }

    /** Isti tekst o šifri u svim mailovima. */
    private String sifraTekst(String sifra, String webUrl) {
        if (sifra == null || sifra.isBlank() || "—".equals(sifra)) return "";
        return "Vaša šifra: " + sifra + "\n\n"
                + "Pod ovom šifrom bićete na preliminarnoj i konačnoj rang listi "
                + "(radi zaštite ličnih podataka liste ne sadrže imena). "
                + "Šifru uvijek možete pogledati na stranici Moj profil: "
                + webUrl + "/student/profil\n\n";
    }

    public void posaljiPromjenuStatusa(String email, String ime, int godina, String status,
                                       String napomena, RezultatBodovanja r, String webUrl) {

        String akGod = godina + "/" + (godina + 1);
        boolean imaNapomenu = napomena != null && !napomena.isBlank();
        String uvod = "Poštovani/a " + ime + ",\n\nvaša prijava za smještaj u Studentskom centru "
                + "Univerziteta u Zenici za akademsku " + akGod + ". godinu ";

        StringBuilder t = new StringBuilder();
        String naslov;

        switch (status) {
            case Statusi.ODOBRENO -> {
                naslov = "prijava za smještaj odobrena";
                t.append(uvod).append("je ODOBRENA.\n\n");
                if (r != null) {
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
                if (imaNapomenu) t.append("Napomena: ").append(napomena).append("\n\n");
            }
            case Statusi.ODBIJENO -> {
                naslov = "prijava za smještaj odbijena";
                t.append(uvod).append("je ODBIJENA.\n\n");
                if (imaNapomenu) t.append("Razlog: ").append(napomena).append("\n\n");
                t.append("Ako naknadno dostavite tražene dokumente, obratite se administraciji doma.\n\n");
            }
            case Statusi.NEPOTPUNA -> {
                naslov = "potrebna dopuna dokumentacije";
                t.append(uvod).append("je pregledana, ali DOKUMENTACIJA NIJE POTPUNA.\n\n");
                if (imaNapomenu) t.append("Šta treba dopuniti:\n").append(napomena).append("\n\n");
                t.append("Prijavite se na ").append(webUrl).append(", na početnoj stranici kliknite ")
                        .append("\"Dodaj dokumente\", priložite tražene dokumente, a zatim kliknite ")
                        .append("\"Pošalji dopunu na pregled\".\n\n");
            }
            case Statusi.NA_CEKANJU -> {
                naslov = "lista čekanja";
                t.append(uvod).append("je pregledana i dokumentacija je uredna, ali još nije dostavljen ")
                        .append("dokaz o ispunjavanju uslova za upis naredne godine studija.\n")
                        .append("Zato se nalazite na LISTI ČEKANJA.\n\n")
                        .append("Kada ispunite uslov, dostavite uvjerenje fakulteta u Studentski centar. ")
                        .append("Pravo na smještaj tada ostvarujete prema poziciji na listi čekanja ")
                        .append("i raspoloživim mjestima.\n\n");
                if (imaNapomenu) t.append("Napomena: ").append(napomena).append("\n\n");
            }
            default -> {
                naslov = "promjena statusa prijave";
                t.append(uvod).append("ima novi status: ").append(status).append(".\n\n");
                if (imaNapomenu) t.append("Napomena: ").append(napomena).append("\n\n");
            }
        }

        t.append("Status prijave i rang listu možete pratiti na ").append(webUrl).append(".\n\n")
                .append("Studentski centar Zenica");

        SimpleMailMessage poruka = new SimpleMailMessage();
        poruka.setFrom(od);
        poruka.setTo(email);
        poruka.setSubject("E-Dom: " + naslov);
        poruka.setText(t.toString());
        mailSender.send(poruka);
    }

    /** Poruka studenta domu; Reply-To = student, pa se može odgovoriti direktno iz inboxa. */
    public void posaljiPorukuDomu(String adresaDoma, String emailStudenta, String imeStudenta,
                                  String sifra, String naslov, String tekst) {
        try {
            MimeMessage poruka = mailSender.createMimeMessage();
            MimeMessageHelper h = new MimeMessageHelper(poruka, "UTF-8");
            h.setFrom(new InternetAddress(od, naziv, "UTF-8"));
            h.setTo(adresaDoma);
            if (emailStudenta != null && !emailStudenta.isBlank()) h.setReplyTo(emailStudenta);
            h.setSubject("E-Dom poruka: " + naslov);
            h.setText("Poruka studenta iz sistema E-Dom\n\n"
                    + "Student: " + imeStudenta + " (" + sifra + ")\n"
                    + "E-mail: " + emailStudenta + "\n\n"
                    + "Naslov: " + naslov + "\n\n"
                    + tekst + "\n\n"
                    + "— Odgovorom na ovaj mail odgovarate direktno studentu.");
            mailSender.send(poruka);
        } catch (Exception e) {
            throw new IllegalStateException("Slanje poruke domu nije uspjelo: " + e.getMessage(), e);
        }
    }
}