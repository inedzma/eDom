package ba.unze.edom.server.service;

import ba.unze.edom.server.dto.KontaktForma;
import ba.unze.edom.server.service.javna.KontaktPorukaService;
import ba.unze.edom.server.service.javna.WebTekstoviService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class KontaktPorukaServiceTest {

    private JavaMailSender mail;
    private WebTekstoviService tekstovi;
    private KontaktPorukaService servis;

    @BeforeEach
    void priprema() {
        mail = mock(JavaMailSender.class);
        tekstovi = mock(WebTekstoviService.class);
        when(tekstovi.sve()).thenReturn(Map.of());
        servis = new KontaktPorukaService(mail, tekstovi);
        ReflectionTestUtils.setField(servis, "posiljalac", "edom@gmail.com");
        ReflectionTestUtils.setField(servis, "primalacIzPostavki", "sekretar@test.ba");
    }

    private static KontaktForma forma(String ime, String email, String tema, String poruka) {
        return new KontaktForma(ime, email, tema, poruka, null);
    }

    private SimpleMailMessage poslanaPoruka() {
        ArgumentCaptor<SimpleMailMessage> c = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mail).send(c.capture());
        return c.getValue();
    }

    @Test
    @DisplayName("Ispravna poruka ide sekretaru, a 'Odgovori' ide pošiljaocu")
    void ispravnaPoruka() {
        servis.posalji(forma("Amra Hadžić", "amra@mail.com", "Dokumenti", "Kada se dostavljaju originali?"), "1.1.1.1");

        SimpleMailMessage m = poslanaPoruka();
        assertThat(m.getTo()).containsExactly("sekretar@test.ba");
        assertThat(m.getReplyTo()).isEqualTo("amra@mail.com");
        assertThat(m.getFrom()).isEqualTo("edom@gmail.com");
        assertThat(m.getSubject()).contains("Dokumenti").contains("Amra Hadžić");
        assertThat(m.getText()).contains("Kada se dostavljaju originali?");
    }

    @Test
    @DisplayName("Bot (popunjeno skriveno polje) – ništa se ne šalje, bez greške")
    void botSeIgnorise() {
        servis.posalji(new KontaktForma("Bot", "bot@spam.com", "Ostalo", "Kupite jeftino odmah!!!", "http://spam"), "2.2.2.2");
        verify(mail, never()).send(any(SimpleMailMessage.class));
    }

    @Test
    @DisplayName("Neispravan e-mail se odbija")
    void losEmail() {
        assertThatThrownBy(() -> servis.posalji(forma("Amra", "amra-bez-at", "Ostalo", "Pitanje o konkursu"), "1.1.1.1"))
                .isInstanceOf(KontaktPorukaService.KontaktGreska.class)
                .hasMessageContaining("E-mail");
        verifyNoInteractions(mail);
    }

    @Test
    @DisplayName("Prekratka poruka se odbija")
    void kratkaPoruka() {
        assertThatThrownBy(() -> servis.posalji(forma("Amra", "amra@mail.com", "Ostalo", "Hej"), "1.1.1.1"))
                .isInstanceOf(KontaktPorukaService.KontaktGreska.class)
                .hasMessageContaining("prekratka");
    }

    @Test
    @DisplayName("Najviše 3 poruke s iste adrese; druga adresa i dalje može")
    void ogranicenjePoAdresi() {
        KontaktForma f = forma("Amra", "amra@mail.com", "Ostalo", "Pitanje o konkursu za smještaj");
        servis.posalji(f, "3.3.3.3");
        servis.posalji(f, "3.3.3.3");
        servis.posalji(f, "3.3.3.3");

        assertThatThrownBy(() -> servis.posalji(f, "3.3.3.3"))
                .isInstanceOf(KontaktPorukaService.KontaktGreska.class)
                .hasMessageContaining("više poruka");

        servis.posalji(f, "4.4.4.4");
        verify(mail, times(4)).send(any(SimpleMailMessage.class));
    }

    @Test
    @DisplayName("Nepoznata tema postaje 'Ostalo'")
    void nepoznataTema() {
        servis.posalji(forma("Amra", "amra@mail.com", "<script>", "Pitanje o konkursu za smještaj"), "1.1.1.1");
        assertThat(poslanaPoruka().getSubject()).contains("Ostalo").doesNotContain("<script>");
    }

    @Test
    @DisplayName("Novi red u imenu ne može 'provaliti' u naslov maila")
    void bezNovihRedovaUNaslovu() {
        servis.posalji(forma("Amra\r\nBcc: svi@svi.com", "amra@mail.com", "Ostalo", "Pitanje o konkursu za smještaj"), "1.1.1.1");
        assertThat(poslanaPoruka().getSubject()).doesNotContain("\n").doesNotContain("\r");
    }

    @Test
    @DisplayName("Ako mail server ne radi, student dobija jasnu poruku s adresom")
    void mailNeRadi() {
        doThrow(new MailSendException("SMTP ne radi")).when(mail).send(any(SimpleMailMessage.class));

        assertThatThrownBy(() -> servis.posalji(forma("Amra", "amra@mail.com", "Ostalo", "Pitanje o konkursu"), "1.1.1.1"))
                .isInstanceOf(KontaktPorukaService.KontaktGreska.class)
                .hasMessageContaining("sekretar@test.ba");
    }

    @Test
    @DisplayName("Bez postavke primaoca, mail ide na adresu koju je admin upisao na stranici")
    void primalacIzTekstaStranice() {
        ReflectionTestUtils.setField(servis, "primalacIzPostavki", "");
        when(tekstovi.sve()).thenReturn(Map.of("pocetna.kontakt.email", "novi.sekretar@unze.ba"));

        servis.posalji(forma("Amra", "amra@mail.com", "Ostalo", "Pitanje o konkursu"), "1.1.1.1");
        assertThat(poslanaPoruka().getTo()).containsExactly("novi.sekretar@unze.ba");
    }
}