package service.export;

import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import dao.StudentDAO;
import dto.PrijavaExportDTo;
import model.Prijava;
import model.Student;

import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;

public class PrijavaHtmlExportService implements ExportService<PrijavaExportDTo>{
    @Override
    public void exportData(java.util.List<PrijavaExportDTo> data, java.io.File file) {
        // Implementacija izvoza Prijava u HTML format
        try {
            // 1️⃣ Kreiranje HTML-a sa ispravljenim CSS-om
            StringBuilder html = new StringBuilder();
            html.append("<!DOCTYPE html>")
                    .append("<html xmlns=\"http://www.w3.org/1999/xhtml\">")
                    .append("<head>")
                    .append("<meta charset=\"UTF-8\"/>")
                    .append("<style>")
                    // BITNO: Font dodijeljen SVIM elementima
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
                    .append("<th>Ukupni bodovi</th>")
                    .append("<th>Status prijave</th>")
                    .append("</tr></thead>");

            html.append("<tbody>");
            for (PrijavaExportDTo p : data) {
                html.append("<tr>")
                        .append("<td>").append(p.getIdPrijave()).append("</td>")
                        .append("<td>").append(escapeHtml(p.getIme())).append("</td>")
                        .append("<td>").append(escapeHtml(p.getPrezime())).append("</td>")
                        .append("<td>").append(escapeHtml(p.getDatumPrijave().toString())).append("</td>")
                        .append("<td>").append(escapeHtml(p.getFakultet())).append("</td>")
                        .append("<td>").append(p.getUkupniBodovi()).append("</td>")
                        .append("<td>").append(p.getStatus()).append("</td>")
                        .append("</tr>");
            }
            html.append("</tbody></table></body></html>");

            // 2️⃣ Kreiranje PDF-a
            try (OutputStream os = new FileOutputStream(file)) {
                PdfRendererBuilder builder = new PdfRendererBuilder();
                builder.useFastMode();

                // Učitavanje fonta iz resources - MORA ostati otvoren dok builder ne završi
                try (InputStream fontStream = StudentHtmlExportService.class.getResourceAsStream("/fonts/DejaVuSans.ttf")) {

                    if (fontStream == null) {
                        throw new RuntimeException("CRVENI ALARM: Font nije nađen na /fonts/DejaVuSans.ttf. Provjeri module-info i rebuildaj projekt.");
                    }

                    builder.useFont(() -> fontStream, "DejaVuSans");

                    // Ostatak koda...
                    builder.withHtmlContent(html.toString(), "");
                    builder.toStream(os);
                    builder.run();
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
            throw new RuntimeException("Greška prilikom generisanja PDF dokumenta: " + e.getMessage(), e);
        }
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
