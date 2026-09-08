package service.export;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import dto.RangListaDTO;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class RangListaHtmlExportService {

    // Metoda prima DVIJE liste: brucosi i viseGodine
    public void exportSplitData(List<RangListaDTO> brucosi, List<RangListaDTO> viseGodine, File file) {
        try {
            DateTimeFormatter df = DateTimeFormatter.ofPattern("dd.MM.yyyy");

            LocalDate datumObjave = LocalDate.now();
            LocalDate pocetakUseljenja = datumObjave.plusDays(4);
            LocalDate krajUseljenja = datumObjave.plusDays(12);

            String today = datumObjave.format(df);
            String pocetakStr = pocetakUseljenja.format(df);
            String krajStr = krajUseljenja.format(df);

            StringBuilder html = new StringBuilder();

            html.append("""
            <!DOCTYPE html>
            <html xmlns="http://www.w3.org/1999/xhtml">
            <head>
            <meta charset="UTF-8"/>
            <style>
                body {
                    font-family: 'DejaVuSans', sans-serif;
                    font-size: 12px;
                    margin: 40px;
                    color: #000;
                    line-height: 1.5;
                }

                .header {
                    font-weight: bold;
                    margin-bottom: 30px;
                }

                .center {
                    text-align: center;
                }

                .italic {
                    font-style: italic;
                }

                table {
                    width: 100%;
                    border-collapse: collapse;
                    margin-top: 10px;
                    margin-bottom: 20px;
                }

                th, td {
                    border: 1px solid #000;
                    padding: 6px;
                    font-size: 11px;
                }

                th {
                    font-weight: bold;
                    text-align: center;
                    background-color: #d9d9d9;
                }

                /* BRUCOŠI – ZELENO */
                .brucosi-table th {
                    background-color: #7cb342;
                }

                /* VIŠE GODINE – PLAVO */
                .vise-godine-table th {
                    background-color: #03a9f4;
                }

                .tabela-naslov {
                    font-weight: bold;
                    margin-top: 20px;
                    margin-bottom: 5px;
                    text-decoration: underline;
                    font-size: 13px;
                }

                .footer {
                    margin-top: 40px;
                    page-break-inside: avoid;
                }
            </style>
            </head>
            <body>
            """);

            html.append("""
            <div class="header">
                JU UNIVERZITET U ZENICI<br/>
                OJ STUDENTSKI CENTAR ZENICA<br/>
                Broj: 09-130-020-_____/25<br/>
                Zenica, %s
            </div>
            """.formatted(today));

            html.append("""
            <p class="center">
            Na osnovu člana 161. stav (2) Statuta Univerziteta u Zenici,
            direktorica Studentskog centra Univerziteta u Zenici, donosim
            </p>

            <p class="center"><b>O D L U K U</b></p>

            <p class="center italic">
            DONOŠENJU PRELIMINARNE LISTE PRIMLJENIH STUDENATA NA SMJEŠTAJ I ISHRANU
            U STUDENTSKOM CENTRU UNIVERZITETA U ZENICI ZA AKADEMSKU
            2025/26. GODINU
            </p>

            <p>
            Na osnovu Odluke o raspisanom Konkursu Upravnog odbora
            "JU Univerziteta u Zenici" broj: 01-01-1-1495/17 od 28.04.2017. godine,
            Odluke Upravnog odbora o usvajanju Kriterija za bodovanje aplikacija
            studenata za prijem na smještaj i ishranu u Studentski centar
            Univerziteta u Zenici u akademskoj 2025/2026. godini broj:
            01-01-1-1549/25 od 14.4.2025. godine, Odluke direktora Studentskog
            centra Univerziteta u Zenici o formiranju Komisije za prijem na
            smještaj i raspored soba, predmetna Komisija je nakon evaluacije
            zaprimljenih aplikacija konstatovala da su pravo na uslovan prijem
            na smještaj–ishranu u Studentskom centru Univerziteta u Zenici
            ostvarili sljedeći studenti:
            </p>
            """);

            /* BRUCOŠI */
            html.append("<div class='tabela-naslov'>BRUCOŠI</div>");
            html.append("<div class='brucosi-table'>");
            html.append(generisiTabeluHtml(brucosi));
            html.append("</div>");

            /* VIŠE GODINE */
            html.append("<br/>");
            html.append("<div class='tabela-naslov'>STUDENTI VIŠIH GODINA</div>");
            html.append("<div class='vise-godine-table'>");
            html.append(generisiTabeluHtml(viseGodine));
            html.append("</div>");

            /* footer */
            html.append("""
            <div class="footer">

            <p>
            Studentski centar Zenica prima 220 studenata, od toga 79 brucoša i 114
            studenata viših godina, te je slobodnih mjesta ostalo još 30.
            Studenti koji su na listi čekanja pravo na smještaj mogu ostvariti
            nakon priloženog dokaza o ispunjavanju uslova za akademsku 2025/2026.
            Neblagovremene i nepotpune prijave nisu uzete u razmatranje.
            </p>

            <p><b>
            Useljenje u Studentski centar sa ove rang liste vršit će se od
            <span style="color:red;">%s – %s</span>,
            svaki radni dan u periodu od 8:30 do 14:00 sati.
            </b></p>

            <p>
            Isteklom ovog roka student gubi pravo na smještaj u tekućoj akademskoj
            godini ukoliko nije odgodio useljenje. Opravdana odgoda useljenja
            vrši se u upravi Studentskog centra.
            </p>

            <p>
            Žalbe na rang listu podnose se pismeno Komisiji za žalbe najkasnije
            7 dana od dana objave. Žalbe slati na adresu:
            Studentski centar Zenica, Crkvice 50, 72000 Zenica.
            Poslije ovog roka žalbe se neće razmatrati.
            </p>

            <p>
            Informacije vezano za useljenje možete dobiti na broj telefona
            032 226 604 ili putem e-maila:
            <u>tehnickisekretar.sc@unze.ba</u>
            </p>

            <p>
            Student u Studentski centar se može useliti samo lično, a prilikom
            useljenja dužan je priložiti:
            </p>

            <p>
            <b>Ljekarsko uvjerenje</b> (ne starije od 6 mjeseci) sa naznakom
            <i>sposoban za kolektivni smještaj</i>.<br/>
            <b>Dokaz o uplati</b> troškova smještaja i ishrane za oktobar
            u iznosu od <b>150,00 KM</b>.
            </p>

            <p>Odluka stupa na snagu danom donošenja.</p>

            <div style="margin-top:50px; display:flex; justify-content:space-between;">
                <div>
                    Dostavljeno:<br/>
                    1x članovima komisije<br/>
                    1x oglasna tabla<br/>
                    1x a/a
                </div>

                <div style="text-align:right;">
                    <b>DIREKTORICA</b><br/>
                    mr.sc. Niždžara Halilović-Čustović
                </div>
            </div>

            </div>
            """.formatted(pocetakStr, krajStr));

            html.append("</body></html>");

            try (OutputStream os = new FileOutputStream(file)) {
                PdfRendererBuilder builder = new PdfRendererBuilder();
                builder.useFastMode();

                try (InputStream fontStream =
                             getClass().getResourceAsStream("/fonts/DejaVuSans.ttf")) {

                    if (fontStream == null) {
                        throw new RuntimeException("Font DejaVuSans.ttf nije pronađen.");
                    }

                    builder.useFont(() -> fontStream, "DejaVuSans");
                    builder.withHtmlContent(html.toString(), "");
                    builder.toStream(os);
                    builder.run();
                }
            }

        } catch (Exception e) {
            throw new RuntimeException("Greška pri exportu rang liste.", e);
        }
    }


    private String generisiTabeluHtml(List<RangListaDTO> data) {
        if (data == null || data.isEmpty()) {
            return "<p class='center'><i>Nema studenata u ovoj kategoriji.</i></p>";
        }

        StringBuilder sb = new StringBuilder();
        sb.append("<table>");

        sb.append("<tr>");
        for (String kolona : data.get(0).getKolone().keySet()) {
            sb.append("<th>").append(escapeHtml(kolona)).append("</th>");
        }
        sb.append("</tr>");

        for (RangListaDTO dto : data) {
            sb.append("<tr>");
            for (String value : dto.getKolone().values()) {
                boolean isNumber = value.matches("-?\\d+([.,]\\d+)?");
                String align = isNumber ? "center" : "left";

                sb.append("<td style='text-align:")
                        .append(align)
                        .append(";'>")
                        .append(escapeHtml(value))
                        .append("</td>");
            }
            sb.append("</tr>");
        }

        sb.append("</table>");
        return sb.toString();
    }

    private String escapeHtml(String text) {
        if (text == null) return "";
        return text.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&#39;");
    }
}
