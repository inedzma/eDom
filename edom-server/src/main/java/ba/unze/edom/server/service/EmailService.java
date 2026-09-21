package ba.unze.edom.server.service;

import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

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
            // ne prekidamo tok zbog maila, ali biljezimo
            log.error("Slanje e-maila na {} nije uspjelo", kome, e);
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
}