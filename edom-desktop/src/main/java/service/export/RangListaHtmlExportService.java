package service.export;

import api.PostavkeApi;
import ba.unze.edom.common.dto.rang.KategorijaRanga;
import ba.unze.edom.common.dto.opste.PostavkeDTO;
import ba.unze.edom.common.dto.rang.RangListaAdminDTO;
import ba.unze.edom.common.dto.rang.RangRedDTO;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Stream;

public class RangListaHtmlExportService {

    private static final DateTimeFormatter DF = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    private static final String FONT = "/fonts/DejaVuSans.ttf";
    private static final String[] MJESECI = {"januar", "februar", "mart", "april", "maj", "juni",
            "juli", "august", "septembar", "oktobar", "novembar", "decembar"};

    public static final List<RangKolona> JAVNE_KOLONE =
            List.of(RangKolona.REDNI_BROJ, RangKolona.SIFRA, RangKolona.UKUPNO);

    /** Sve tri liste u jednoj, po bodovima. Koristi ga i ekran ("Sve") i PDF. */
    public static List<RangRedDTO> objedinjeno(RangListaAdminDTO l) {
        return Stream.of(l.brucosi(), l.saUslovom(), l.listaCekanja())
                .flatMap(List::stream)
                .sorted(Comparator.comparing((RangRedDTO r) -> r.greska() != null)
                        .thenComparing(RangRedDTO::ukupno, Comparator.reverseOrder())
                        .thenComparing(RangRedDTO::idPrijava))
                .toList();
    }

    public static List<RangRedDTO> redovi(RangListaAdminDTO l, KategorijaRanga k) {
        return switch (k) {
            case BRUCOS -> l.brucosi();
            case SA_USLOVOM -> l.saUslovom();
            case LISTA_CEKANJA -> l.listaCekanja();
        };
    }

    /** @return broj prijava izostavljenih zbog neispravnih podataka */
    public int export(RangListaAdminDTO lista, ExportOpcije o, File file) {
        if (getClass().getResource(FONT) == null) {
            throw new IllegalStateException("Font DejaVuSans.ttf nije pronađen.");
        }
        PostavkeDTO p = PostavkeApi.ucitaj();   // telefon, direktorica, odluke… iz Postavki
        String html = sastaviHtml(lista, o, p);

        try (OutputStream os = new FileOutputStream(file)) {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.useFastMode();
            builder.useFont(() -> {
                InputStream in = getClass().getResourceAsStream(FONT);
                if (in == null) throw new IllegalStateException("Font DejaVuSans.ttf nije pronađen.");
                return in;
            }, "DejaVuSans");
            builder.withHtmlContent(html, "");
            builder.toStream(os);
            builder.run();
        } catch (Exception e) {
            throw new RuntimeException("Greška pri exportu rang liste.", e);
        }
        return izostavljeno(lista, o);
    }

    // ---------- HTML ----------

    private String sastaviHtml(RangListaAdminDTO lista, ExportOpcije o, PostavkeDTO p) {
        List<RangKolona> kolone = o.kolone();
        String akGod = kratkaGodina(lista.akademskaGodina());
        String danas = LocalDate.now().format(DF);
        String vrsta = o.konacna() ? "KONAČNE" : "PRELIMINARNE";
        String funkcija = n(p.direktorFunkcija(), "DIREKTORICA");

        StringBuilder html = new StringBuilder();
        html.append(head(kolone.size() > 6));

        html.append("""
                <div class="header">
                    JU UNIVERZITET U ZENICI<br/>
                    OJ STUDENTSKI CENTAR ZENICA<br/>
                    Broj: %s-_____/%02d<br/>
                    Zenica, %s
                </div>
                """.formatted(esc(n(p.brojRangListe(), "09-130-020")), LocalDate.now().getYear() % 100, danas));

        html.append("""
                <p class="center">
                Na osnovu člana 161. stav (2) Statuta Univerziteta u Zenici,
                %s Studentskog centra Univerziteta u Zenici, donosim
                </p>
                <p class="center"><b>O D L U K U</b></p>
                <p class="center italic">
                O DONOŠENJU %s LISTE PRIMLJENIH STUDENATA NA SMJEŠTAJ I ISHRANU
                U STUDENTSKOM CENTRU UNIVERZITETA U ZENICI ZA AKADEMSKU %s. GODINU
                </p>
                <p>
                Na osnovu Odluke o raspisanom Konkursu Upravnog odbora
                "JU Univerziteta u Zenici" broj: %s godine,
                Odluke Upravnog odbora o usvajanju Kriterija za bodovanje aplikacija
                studenata za prijem na smještaj i ishranu u Studentski centar
                Univerziteta u Zenici u akademskoj %s. godini broj: %s godine,
                Odluke direktora Studentskog centra Univerziteta u Zenici o formiranju
                Komisije za prijem na smještaj i raspored soba, predmetna Komisija je
                nakon evaluacije zaprimljenih aplikacija konstatovala da su pravo na
                %s na smještaj–ishranu u Studentskom centru Univerziteta u Zenici
                ostvarili sljedeći studenti:
                </p>
                """.formatted(funkcija.toLowerCase(Locale.ROOT), vrsta, esc(akGod),
                esc(n(p.brojOdlukeKonkurs(), "")), esc(akGod), esc(n(p.brojOdlukeKriteriji(), "")),
                o.konacna() ? "prijem" : "uslovan prijem"));

        for (KategorijaRanga k : KategorijaRanga.values()) {
            if (!o.liste().contains(k)) continue;
            List<RangRedDTO> redovi = redovi(lista, k).stream()
                    .filter(r -> r.greska() == null)
                    .filter(r -> !o.samoPrimljeni() || k == KategorijaRanga.LISTA_CEKANJA || r.primljen())
                    .toList();
            html.append("<div class='tabela-naslov'>").append(esc(k.naslovPdf())).append("</div>");
            html.append(tabela(redovi, kolone, cssKlasa(k)));
        }

        if (o.objedinjeno()) {
            List<RangKolona> saKategorijom = new ArrayList<>(kolone);
            if (!saKategorijom.contains(RangKolona.KATEGORIJA)) {
                saKategorijom.add(Math.min(2, saKategorijom.size()), RangKolona.KATEGORIJA);
            }
            List<RangRedDTO> svi = objedinjeno(lista).stream().filter(r -> r.greska() == null).toList();
            html.append("<div class='tabela-naslov'>OBJEDINJENA LISTA</div>");
            html.append(tabela(svi, saKategorijom, "objedinjeno-table"));
        }

        html.append(podnozje(lista, o, akGod, p));
        html.append("</body></html>");
        return html.toString();
    }

    private String head(boolean siroko) {
        return """
                <!DOCTYPE html>
                <html xmlns="http://www.w3.org/1999/xhtml">
                <head>
                <meta charset="UTF-8"/>
                <style>
                    @page { size: A4 %s; margin: 18mm; }
                    body { font-family: 'DejaVuSans', sans-serif; font-size: 12px; color: #000; line-height: 1.5; }
                    .header { font-weight: bold; margin-bottom: 30px; }
                    .center { text-align: center; }
                    .italic { font-style: italic; }
                    table { width: 100%%; border-collapse: collapse; margin-top: 10px; margin-bottom: 20px; }
                    th, td { border: 1px solid #000; padding: 5px; font-size: 11px; }
                    th { font-weight: bold; text-align: center; background-color: #d9d9d9; }
                    tr { page-break-inside: avoid; }
                    thead { display: table-header-group; }
                    .brucosi-table th { background-color: #7cb342; }
                    .vise-godine-table th { background-color: #03a9f4; }
                    .cekanje-table th { background-color: #ffb74d; }
                    .objedinjeno-table th { background-color: #bdbdbd; }
                    .tabela-naslov { font-weight: bold; margin-top: 20px; margin-bottom: 5px;
                                     text-decoration: underline; font-size: 13px; }
                    .footer { margin-top: 40px; }
                    table.potpis, table.potpis td { border: none; padding: 0; font-size: 12px; }
                </style>
                </head>
                <body>
                """.formatted(siroko ? "landscape" : "portrait");
    }

    private String tabela(List<RangRedDTO> redovi, List<RangKolona> kolone, String klasa) {
        if (redovi.isEmpty()) {
            return "<p class='center'><i>Nema studenata u ovoj kategoriji.</i></p>";
        }
        StringBuilder sb = new StringBuilder("<table class='").append(klasa).append("'><thead><tr>");
        for (RangKolona k : kolone) {
            sb.append("<th>").append(esc(k.naslov())).append("</th>");
        }
        sb.append("</tr></thead><tbody>");

        int rb = 1;
        for (RangRedDTO r : redovi) {
            sb.append("<tr>");
            for (RangKolona k : kolone) {
                sb.append(k.centrirano() ? "<td style='text-align:center;'>" : "<td>")
                        .append(esc(k.vrijednost(r, rb)))
                        .append("</td>");
            }
            sb.append("</tr>");
            rb++;
        }
        return sb.append("</tbody></table>").toString();
    }

    private String podnozje(RangListaAdminDTO l, ExportOpcije o, String akGod, PostavkeDTO p) {
        String mjesec = MJESECI[o.useljenjeOd().getMonthValue() - 1];
        return """
                <div class="footer">
                <p>
                Studentski centar Zenica prima %d studenata, od toga %d brucoša i %d
                studenata viših godina, te je slobodnih mjesta ostalo još %d.
                Studenti koji su na listi čekanja pravo na smještaj mogu ostvariti
                nakon priloženog dokaza o ispunjavanju uslova za akademsku %s. godinu.
                Neblagovremene i nepotpune prijave nisu uzete u razmatranje.
                </p>
                <p><b>
                Useljenje u Studentski centar sa ove rang liste vršit će se od
                <span style="color:red;">%s – %s</span>,
                svaki radni dan u periodu od %s sati.
                </b></p>
                <p>
                Isteklom ovog roka student gubi pravo na smještaj u tekućoj akademskoj
                godini ukoliko nije odgodio useljenje. Opravdana odgoda useljenja
                vrši se u upravi Studentskog centra.
                </p>
                <p>
                Žalbe na rang listu podnose se pismeno Komisiji za žalbe najkasnije
                %d dana od dana objave. Žalbe slati na adresu:
                Studentski centar Zenica, %s.
                Poslije ovog roka žalbe se neće razmatrati.
                </p>
                <p>
                Informacije vezano za useljenje možete dobiti na broj telefona
                %s ili putem e-maila: <u>%s</u>
                </p>
                <p>
                Student u Studentski centar se može useliti samo lično, a prilikom
                useljenja dužan je priložiti:
                </p>
                <p>
                <b>Ljekarsko uvjerenje</b> (ne starije od 6 mjeseci) sa naznakom
                <i>sposoban za kolektivni smještaj</i>.<br/>
                <b>Dokaz o uplati</b> troškova smještaja i ishrane za %s
                u iznosu od <b>%s KM</b>.
                </p>
                <p>Odluka stupa na snagu danom donošenja.</p>

                <table class="potpis" style="margin-top:50px;">
                  <tr>
                    <td style="width:50%%;">
                      Dostavljeno:<br/>1x članovima komisije<br/>1x oglasna tabla<br/>1x a/a
                    </td>
                    <td style="width:50%%; text-align:right; vertical-align:top;">
                      <b>%s</b><br/>%s
                    </td>
                  </tr>
                </table>
                </div>
                """.formatted(l.kapacitet(), l.primljenoBrucosa(), l.primljenoVisih(), l.slobodno(),
                esc(akGod), o.useljenjeOd().format(DF), o.useljenjeDo().format(DF),
                esc(n(p.useljenjeRadnoVrijeme(), "8:30 – 14:00")),
                p.rokZalbeDana() != null ? p.rokZalbeDana() : 7,
                esc(n(p.adresa(), "Crkvice 50, 72000 Zenica")),
                esc(n(p.telefon(), "")), esc(n(p.email(), "")),
                mjesec, esc(n(p.uplataPriUseljenju(), "150,00")),
                esc(n(p.direktorFunkcija(), "DIREKTORICA")), esc(n(p.direktor(), "")));
    }

    // ---------- pomoćne ----------

    private int izostavljeno(RangListaAdminDTO l, ExportOpcije o) {
        Set<Integer> ids = new HashSet<>();
        for (KategorijaRanga k : KategorijaRanga.values()) {
            if (o.objedinjeno() || o.liste().contains(k)) {
                redovi(l, k).stream().filter(r -> r.greska() != null).forEach(r -> ids.add(r.idPrijava()));
            }
        }
        return ids.size();
    }

    private String cssKlasa(KategorijaRanga k) {
        return switch (k) {
            case BRUCOS -> "brucosi-table";
            case SA_USLOVOM -> "vise-godine-table";
            case LISTA_CEKANJA -> "cekanje-table";
        };
    }

    /** "2026/2027" → "2026/27" */
    private String kratkaGodina(String ak) {
        if (ak == null) return "";
        String[] d = ak.split("/");
        return d.length == 2 && d[1].length() == 4 ? d[0] + "/" + d[1].substring(2) : ak;
    }

    private static String n(String s, String zamjena) {
        return s == null || s.isBlank() ? zamjena : s;
    }

    private static String esc(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&#39;");
    }
}