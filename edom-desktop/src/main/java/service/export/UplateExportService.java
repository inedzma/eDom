package service.export;

import ba.unze.edom.common.dto.uplate.UplateTabelaDTO;
import ba.unze.edom.common.dto.uplate.UplateTabelaDTO.Celija;
import ba.unze.edom.common.dto.uplate.UplateTabelaDTO.Red;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Izvještaj o uplatama: PDF (za štampu) i Excel (.xlsx, za dalju obradu).
 * Izvozi se ono što je na ekranu: ista godina, filter, pretraga i redoslijed.
 *
 * Excel se pravi bez dodatne biblioteke: .xlsx je zip s nekoliko XML fajlova.
 */
public class UplateExportService {

    private static final String[] RIMSKI = {"", "I", "II", "III", "IV", "V", "VI", "VII", "VIII", "IX", "X", "XI", "XII"};
    private static final DateTimeFormatter KRATKO = DateTimeFormatter.ofPattern("dd.MM.");
    private static final DateTimeFormatter PUNO = DateTimeFormatter.ofPattern("dd.MM.yyyy.");

    // =====================================================================
    // PDF
    // =====================================================================

    /** @param prikaz opis filtera za zaglavlje, npr. "Svi studenti" ili "Duguju" */
    public void pdf(UplateTabelaDTO podaci, List<Red> redovi, String prikaz, File fajl) {
        StringBuilder h = new StringBuilder()
                .append("<!DOCTYPE html><html xmlns=\"http://www.w3.org/1999/xhtml\"><head><meta charset=\"UTF-8\"/><style>")
                .append("@page { size: A4 landscape; margin: 14mm 12mm; }")
                .append("body, table, td, th, h1, p { font-family: 'DejaVuSans', sans-serif; }")
                .append("body { font-size: 10px; color: #0b3b47; }")
                .append("h1 { font-size: 16px; margin: 0 0 2px 0; }")
                .append("p.pod { margin: 0 0 10px 0; color: #5c7380; }")
                .append("table { width: 100%; border-collapse: collapse; }")
                .append("th { background: #eef3f4; font-weight: bold; padding: 5px 4px; border: 1px solid #cfdde1; }")
                .append("td { padding: 4px; border: 1px solid #dbe5e8; text-align: center; }")
                .append("td.ime { text-align: left; }")
                .append("td.ok { background: #e2f6ef; color: #1e7a4f; }")
                .append("td.dug { background: #fdecea; color: #b3261e; font-weight: bold; }")
                .append("td.van { color: #b8c8cd; }")
                .append("p.leg { margin-top: 10px; color: #5c7380; }")
                .append("</style></head><body>");

        h.append("<h1>Uplate za smještaj – akademska godina ")
                .append(podaci.akademskaGodina()).append("/").append(podaci.akademskaGodina() + 1).append("</h1>")
                .append("<p class=\"pod\">").append(esc(prikaz)).append(" · ").append(redovi.size()).append(" studenata · ")
                .append(redovi.stream().filter(r -> r.dug() > 0).count()).append(" duguje (ukupno ")
                .append(redovi.stream().mapToInt(Red::dug).sum()).append(" mj.) · izvještaj od ")
                .append(LocalDate.now().format(PUNO)).append("</p>");

        h.append("<table><thead><tr><th>Student</th><th>Soba</th>");
        for (String m : podaci.mjeseci()) h.append("<th>").append(rimski(m)).append("</th>");
        h.append("<th>Dug</th></tr></thead><tbody>");

        for (Red r : redovi) {
            h.append("<tr><td class=\"ime\">").append(esc(r.student())).append("</td>")
                    .append("<td>").append(r.soba() != null ? r.soba() : "–").append("</td>");
            for (Celija c : r.celije()) {
                h.append("<td class=\"").append(klasa(c)).append("\">").append(esc(oznaka(c))).append("</td>");
            }
            h.append("<td class=\"").append(r.dug() > 0 ? "dug" : "ok").append("\">")
                    .append(r.dug() > 0 ? r.dug() + " mj." : "uredno").append("</td></tr>");
        }
        h.append("</tbody></table>")
                .append("<p class=\"leg\">✔ plaćeno (datum uplate) · ✖ nije plaćeno · • tekući ili budući mjesec · – student tada nije bio u domu</p>")
                .append("</body></html>");

        try (OutputStream os = new FileOutputStream(fajl);
             InputStream font = getClass().getResourceAsStream("/fonts/DejaVuSans.ttf")) {
            if (font == null) throw new IllegalStateException("Font nije nađen na /fonts/DejaVuSans.ttf.");
            PdfRendererBuilder b = new PdfRendererBuilder();
            b.useFastMode();
            b.useFont(() -> font, "DejaVuSans");
            b.withHtmlContent(h.toString(), "");
            b.toStream(os);
            b.run();
        } catch (Exception e) {
            throw new RuntimeException("Greška prilikom pravljenja PDF-a: " + e.getMessage(), e);
        }
    }

    // =====================================================================
    // EXCEL (.xlsx)
    // =====================================================================

    // stilovi u styles.xml: 0 obično, 1 zaglavlje, 2 plaćeno, 3 dug, 4 naslov, 5 sivo
    private static final int S_OBICNO = 0, S_ZAGLAVLJE = 1, S_PLACENO = 2, S_DUG = 3, S_NASLOV = 4, S_SIVO = 5;

    public void excel(UplateTabelaDTO podaci, List<Red> redovi, String prikaz, File fajl) {
        int g = podaci.akademskaGodina();
        int kolona = podaci.mjeseci().size() + 3;          // student, soba, mjeseci, dug
        StringBuilder rows = new StringBuilder();
        int red = 1;

        rows.append(redXml(red++, new String[]{"Uplate za smještaj – akademska godina " + g + "/" + (g + 1)}, new int[]{S_NASLOV}));
        rows.append(redXml(red++, new String[]{prikaz + " · izvještaj od " + LocalDate.now().format(PUNO)}, new int[]{S_SIVO}));
        red++;                                               // prazan red

        String[] zag = new String[kolona];
        int[] zs = new int[kolona];
        zag[0] = "Student"; zag[1] = "Soba";
        for (int i = 0; i < podaci.mjeseci().size(); i++) zag[i + 2] = rimski(podaci.mjeseci().get(i));
        zag[kolona - 1] = "Dug (mj.)";
        java.util.Arrays.fill(zs, S_ZAGLAVLJE);
        int redZaglavlja = red;
        rows.append(redXml(red++, zag, zs));

        for (Red r : redovi) {
            String[] v = new String[kolona];
            int[] s = new int[kolona];
            v[0] = r.student();
            v[1] = r.soba() != null ? "#" + r.soba() : "";
            for (int i = 0; i < r.celije().size(); i++) {
                Celija c = r.celije().get(i);
                v[i + 2] = UplateTabelaDTO.PLACENO.equals(c.stanje()) ? c.datum().format(PUNO)
                        : UplateTabelaDTO.DUG.equals(c.stanje()) ? "nije plaćeno"
                        : "";
                s[i + 2] = UplateTabelaDTO.PLACENO.equals(c.stanje()) ? S_PLACENO
                        : UplateTabelaDTO.DUG.equals(c.stanje()) ? S_DUG : S_OBICNO;
            }
            v[kolona - 1] = "#" + r.dug();                   // "#" = broj, ne tekst
            s[kolona - 1] = r.dug() > 0 ? S_DUG : S_OBICNO;
            rows.append(redXml(red++, v, s));
        }

        StringBuilder cols = new StringBuilder("<cols><col min=\"1\" max=\"1\" width=\"30\" customWidth=\"1\"/>")
                .append("<col min=\"2\" max=\"2\" width=\"7\" customWidth=\"1\"/>")
                .append("<col min=\"3\" max=\"").append(kolona - 1).append("\" width=\"12\" customWidth=\"1\"/>")
                .append("<col min=\"").append(kolona).append("\" max=\"").append(kolona).append("\" width=\"10\" customWidth=\"1\"/></cols>");

        String sheet = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
                + "<worksheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\">"
                + "<sheetViews><sheetView workbookViewId=\"0\"><pane ySplit=\"" + redZaglavlja + "\" topLeftCell=\"A"
                + (redZaglavlja + 1) + "\" activePane=\"bottomLeft\" state=\"frozen\"/></sheetView></sheetViews>"
                + cols + "<sheetData>" + rows + "</sheetData>"
                + "<autoFilter ref=\"A" + redZaglavlja + ":" + slovo(kolona) + Math.max(redZaglavlja, red - 1) + "\"/>"
                + "</worksheet>";

        try (ZipOutputStream zip = new ZipOutputStream(new FileOutputStream(fajl), StandardCharsets.UTF_8)) {
            dodaj(zip, "[Content_Types].xml", CONTENT_TYPES);
            dodaj(zip, "_rels/.rels", RELS);
            dodaj(zip, "xl/workbook.xml", WORKBOOK.replace("{NAZIV}", "Uplate " + g + "-" + (g + 1)));
            dodaj(zip, "xl/_rels/workbook.xml.rels", WORKBOOK_RELS);
            dodaj(zip, "xl/styles.xml", STYLES);
            dodaj(zip, "xl/worksheets/sheet1.xml", sheet);
        } catch (IOException e) {
            throw new RuntimeException("Greška prilikom pravljenja Excel fajla: " + e.getMessage(), e);
        }
    }

    /** Jedan red tabele; vrijednost koja počinje s "#" upisuje se kao broj. */
    private static String redXml(int red, String[] vrijednosti, int[] stilovi) {
        StringBuilder x = new StringBuilder("<row r=\"").append(red).append("\">");
        for (int i = 0; i < vrijednosti.length; i++) {
            String v = vrijednosti[i];
            String ref = slovo(i + 1) + red;
            int s = i < stilovi.length ? stilovi[i] : S_OBICNO;
            if (v == null) continue;
            if (v.startsWith("#")) {
                x.append("<c r=\"").append(ref).append("\" s=\"").append(s).append("\"><v>")
                        .append(v.substring(1)).append("</v></c>");
            } else {
                x.append("<c r=\"").append(ref).append("\" s=\"").append(s).append("\" t=\"inlineStr\"><is><t>")
                        .append(esc(v)).append("</t></is></c>");
            }
        }
        return x.append("</row>").toString();
    }

    /** 1 → A, 2 → B, … 27 → AA */
    private static String slovo(int kolona) {
        StringBuilder s = new StringBuilder();
        for (int k = kolona; k > 0; k = (k - 1) / 26) s.insert(0, (char) ('A' + (k - 1) % 26));
        return s.toString();
    }

    private static void dodaj(ZipOutputStream zip, String ime, String sadrzaj) throws IOException {
        zip.putNextEntry(new ZipEntry(ime));
        zip.write(sadrzaj.getBytes(StandardCharsets.UTF_8));
        zip.closeEntry();
    }

    private static final String CONTENT_TYPES = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
            + "<Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\">"
            + "<Default Extension=\"rels\" ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/>"
            + "<Default Extension=\"xml\" ContentType=\"application/xml\"/>"
            + "<Override PartName=\"/xl/workbook.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml\"/>"
            + "<Override PartName=\"/xl/worksheets/sheet1.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml\"/>"
            + "<Override PartName=\"/xl/styles.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml\"/>"
            + "</Types>";

    private static final String RELS = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
            + "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">"
            + "<Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument\" Target=\"xl/workbook.xml\"/>"
            + "</Relationships>";

    private static final String WORKBOOK = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
            + "<workbook xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\" "
            + "xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\">"
            + "<sheets><sheet name=\"{NAZIV}\" sheetId=\"1\" r:id=\"rId1\"/></sheets>"
            + "</workbook>";

    private static final String WORKBOOK_RELS = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
            + "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">"
            + "<Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet\" Target=\"worksheets/sheet1.xml\"/>"
            + "<Relationship Id=\"rId2\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles\" Target=\"styles.xml\"/>"
            + "</Relationships>";

    /** 0 obično, 1 zaglavlje (bold, sivo), 2 plaćeno (zeleno), 3 dug (crveno), 4 naslov, 5 sivi tekst */
    private static final String STYLES = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>"
            + "<styleSheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\">"
            + "<fonts count=\"5\">"
            + "<font><sz val=\"11\"/><name val=\"Calibri\"/></font>"
            + "<font><b/><sz val=\"11\"/><name val=\"Calibri\"/></font>"
            + "<font><sz val=\"11\"/><color rgb=\"FF1E7A4F\"/><name val=\"Calibri\"/></font>"
            + "<font><b/><sz val=\"11\"/><color rgb=\"FFB3261E\"/><name val=\"Calibri\"/></font>"
            + "<font><b/><sz val=\"14\"/><name val=\"Calibri\"/></font>"
            + "</fonts>"
            + "<fills count=\"5\">"
            + "<fill><patternFill patternType=\"none\"/></fill>"
            + "<fill><patternFill patternType=\"gray125\"/></fill>"
            + "<fill><patternFill patternType=\"solid\"><fgColor rgb=\"FFEEF3F4\"/></patternFill></fill>"
            + "<fill><patternFill patternType=\"solid\"><fgColor rgb=\"FFE2F6EF\"/></patternFill></fill>"
            + "<fill><patternFill patternType=\"solid\"><fgColor rgb=\"FFFDECEA\"/></patternFill></fill>"
            + "</fills>"
            + "<borders count=\"2\"><border/>"
            + "<border><left style=\"thin\"><color rgb=\"FFCFDDE1\"/></left><right style=\"thin\"><color rgb=\"FFCFDDE1\"/></right>"
            + "<top style=\"thin\"><color rgb=\"FFCFDDE1\"/></top><bottom style=\"thin\"><color rgb=\"FFCFDDE1\"/></bottom></border>"
            + "</borders>"
            + "<cellStyleXfs count=\"1\"><xf numFmtId=\"0\" fontId=\"0\" fillId=\"0\" borderId=\"0\"/></cellStyleXfs>"
            + "<cellXfs count=\"6\">"
            + "<xf numFmtId=\"0\" fontId=\"0\" fillId=\"0\" borderId=\"1\" xfId=\"0\" applyBorder=\"1\"/>"
            + "<xf numFmtId=\"0\" fontId=\"1\" fillId=\"2\" borderId=\"1\" xfId=\"0\" applyFont=\"1\" applyFill=\"1\" applyBorder=\"1\" applyAlignment=\"1\"><alignment horizontal=\"center\"/></xf>"
            + "<xf numFmtId=\"0\" fontId=\"2\" fillId=\"3\" borderId=\"1\" xfId=\"0\" applyFont=\"1\" applyFill=\"1\" applyBorder=\"1\" applyAlignment=\"1\"><alignment horizontal=\"center\"/></xf>"
            + "<xf numFmtId=\"0\" fontId=\"3\" fillId=\"4\" borderId=\"1\" xfId=\"0\" applyFont=\"1\" applyFill=\"1\" applyBorder=\"1\" applyAlignment=\"1\"><alignment horizontal=\"center\"/></xf>"
            + "<xf numFmtId=\"0\" fontId=\"4\" fillId=\"0\" borderId=\"0\" xfId=\"0\" applyFont=\"1\"/>"
            + "<xf numFmtId=\"0\" fontId=\"0\" fillId=\"0\" borderId=\"0\" xfId=\"0\"/>"
            + "</cellXfs>"
            + "<cellStyles count=\"1\"><cellStyle name=\"Normal\" xfId=\"0\" builtinId=\"0\"/></cellStyles>"
            + "</styleSheet>";

    // =====================================================================
    // zajedničko
    // =====================================================================

    private static String rimski(String mjesec) {
        YearMonth m = YearMonth.parse(mjesec);
        return RIMSKI[m.getMonthValue()] + " " + m.getYear();
    }

    private static String oznaka(Celija c) {
        return switch (c.stanje()) {
            case UplateTabelaDTO.PLACENO -> "✔ " + c.datum().format(KRATKO);
            case UplateTabelaDTO.DUG -> "✖";
            case UplateTabelaDTO.CEKA -> "•";
            default -> "–";
        };
    }

    private static String klasa(Celija c) {
        return switch (c.stanje()) {
            case UplateTabelaDTO.PLACENO -> "ok";
            case UplateTabelaDTO.DUG -> "dug";
            default -> "van";
        };
    }

    private static String esc(String t) {
        if (t == null) return "";
        return t.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")
                .replace("\"", "&quot;").replace("'", "&#39;");
    }
}