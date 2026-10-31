package service.export;

import ba.unze.edom.common.dto.prijava.PrijavaListaDTO;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class PrijavaHtmlExportService implements ExportService<PrijavaListaDTO> {

    private static final DateTimeFormatter DATUM = DateTimeFormatter.ofPattern("dd.MM.yyyy.");

    @Override
    public void exportData(List<PrijavaListaDTO> data, File file) {
        try {
            StringBuilder html = new StringBuilder();
            html.append("<!DOCTYPE html>")
                    .append("<html xmlns=\"http://www.w3.org/1999/xhtml\">")
                    .append("<head>")
                    .append("<meta charset=\"UTF-8\"/>")
                    .append("<style>")
                    .append("body, table, td, th, h1 { font-family: 'DejaVuSans', sans-serif; }")
                    .append("body { margin: 20px; font-size: 12px; }")
                    .append("h1 { text-align: center; color: #333; }")
                    .append("table { width: 100%; border-collapse: collapse; margin-top: 20px; }")
                    .append("th, td { border: 1px solid #000; padding: 8px; text-align: left; }")
                    .append("th { background-color: #f2f2f2; font-weight: bold; }")
                    .append("tr:nth-child(even) { background-color: #f9f9f9; }")
                    .append("</style>")
                    .append("</head>")
                    .append("<body>");

            html.append("<h1>Lista prijava</h1>");
            html.append("<table>");
            html.append("<thead><tr>")
                    .append("<th>ID</th>")
                    .append("<th>Ime</th>")
                    .append("<th>Prezime</th>")
                    .append("<th>Datum prijave</th>")
                    .append("<th>Fakultet</th>")
                    .append("<th>Bodovi (potvrđeno / moguće)</th>")
                    .append("<th>Status prijave</th>")
                    .append("</tr></thead>");

            html.append("<tbody>");
            for (PrijavaListaDTO p : data) {
                html.append("<tr>")
                        .append("<td>").append(p.idPrijava()).append("</td>")
                        .append("<td>").append(escapeHtml(p.ime())).append("</td>")
                        .append("<td>").append(escapeHtml(p.prezime())).append("</td>")
                        .append("<td>").append(p.datumPrijave() != null ? p.datumPrijave().format(DATUM) : "").append("</td>")
                        .append("<td>").append(escapeHtml(p.fakultet())).append("</td>")
                        .append("<td>").append(escapeHtml(bodovi(p))).append("</td>")
                        .append("<td>").append(escapeHtml(p.status())).append("</td>")
                        .append("</tr>");
            }
            html.append("</tbody></table></body></html>");

            try (OutputStream os = new FileOutputStream(file)) {
                PdfRendererBuilder builder = new PdfRendererBuilder();
                builder.useFastMode();

                try (InputStream fontStream = getClass().getResourceAsStream("/fonts/DejaVuSans.ttf")) {

                    if (fontStream == null) {
                        throw new RuntimeException("Font nije nađen na /fonts/DejaVuSans.ttf.");
                    }

                    builder.useFont(() -> fontStream, "DejaVuSans");
                    builder.withHtmlContent(html.toString(), "");
                    builder.toStream(os);
                    builder.run();
                }
            }

        } catch (Exception e) {
            throw new RuntimeException("Greška prilikom generisanja PDF dokumenta: " + e.getMessage(), e);
        }
    }

    /** Isti prikaz kao u tabeli na ekranu. */
    private String bodovi(PrijavaListaDTO p) {
        if (p.greska() != null) return "neispravni podaci";
        if (p.potvrdjeniBodovi() == null) return "";
        return String.format("%.2f / %.2f", p.potvrdjeniBodovi(), p.potencijalniBodovi());
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