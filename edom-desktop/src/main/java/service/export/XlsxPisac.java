package service.export;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/** Jednostavan .xlsx bez biblioteka: listovi s podebljanim, zamrznutim zaglavljem. */
public final class XlsxPisac {

    /** Jedan list: naslovi kolona, širine (u znakovima) i redovi (Number = broj, ostalo = tekst). */
    public record Tabela(String ime, List<String> naslovi, List<Integer> sirine, List<List<Object>> redovi) {}

    private static final String XML = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n";
    private static final String NS_MAIN = "http://schemas.openxmlformats.org/spreadsheetml/2006/main";
    private static final String NS_REL = "http://schemas.openxmlformats.org/officeDocument/2006/relationships";
    private static final String NS_PKG_REL = "http://schemas.openxmlformats.org/package/2006/relationships";

    private static final String STYLES = XML
            + "<styleSheet xmlns=\"" + NS_MAIN + "\">"
            + "<fonts count=\"2\"><font><sz val=\"11\"/><name val=\"Calibri\"/></font>"
            + "<font><b/><sz val=\"11\"/><name val=\"Calibri\"/></font></fonts>"
            + "<fills count=\"2\"><fill><patternFill patternType=\"none\"/></fill>"
            + "<fill><patternFill patternType=\"gray125\"/></fill></fills>"
            + "<borders count=\"1\"><border><left/><right/><top/><bottom/><diagonal/></border></borders>"
            + "<cellStyleXfs count=\"1\"><xf numFmtId=\"0\" fontId=\"0\" fillId=\"0\" borderId=\"0\"/></cellStyleXfs>"
            + "<cellXfs count=\"3\">"
            + "<xf numFmtId=\"0\" fontId=\"0\" fillId=\"0\" borderId=\"0\" xfId=\"0\"/>"
            + "<xf numFmtId=\"0\" fontId=\"1\" fillId=\"0\" borderId=\"0\" xfId=\"0\" applyFont=\"1\"/>"
            + "<xf numFmtId=\"2\" fontId=\"0\" fillId=\"0\" borderId=\"0\" xfId=\"0\" applyNumberFormat=\"1\"/>"
            + "</cellXfs></styleSheet>";

    private XlsxPisac() {}

    public static void zapisi(File file, List<Tabela> tabele) {
        try (ZipOutputStream zip = new ZipOutputStream(new FileOutputStream(file))) {
            upisi(zip, "[Content_Types].xml", contentTypes(tabele.size()));
            upisi(zip, "_rels/.rels", XML + "<Relationships xmlns=\"" + NS_PKG_REL + "\">"
                    + "<Relationship Id=\"rId1\" Type=\"" + NS_REL + "/officeDocument\" Target=\"xl/workbook.xml\"/>"
                    + "</Relationships>");
            upisi(zip, "xl/workbook.xml", workbook(tabele));
            upisi(zip, "xl/_rels/workbook.xml.rels", workbookRels(tabele.size()));
            upisi(zip, "xl/styles.xml", STYLES);
            for (int i = 0; i < tabele.size(); i++) {
                upisi(zip, "xl/worksheets/sheet" + (i + 1) + ".xml", list(tabele.get(i)));
            }
        } catch (IOException e) {
            throw new RuntimeException("Greška pri izvozu u Excel.", e);
        }
    }

    private static String list(Tabela t) {
        StringBuilder sb = new StringBuilder(XML)
                .append("<worksheet xmlns=\"").append(NS_MAIN).append("\">")
                .append("<sheetViews><sheetView workbookViewId=\"0\">")
                .append("<pane ySplit=\"1\" topLeftCell=\"A2\" activePane=\"bottomLeft\" state=\"frozen\"/>")
                .append("</sheetView></sheetViews><cols>");
        for (int c = 0; c < t.naslovi().size(); c++) {
            int w = c < t.sirine().size() ? t.sirine().get(c) : 12;
            sb.append("<col min=\"").append(c + 1).append("\" max=\"").append(c + 1)
                    .append("\" width=\"").append(w).append("\" customWidth=\"1\"/>");
        }
        sb.append("</cols><sheetData><row r=\"1\">");
        for (int c = 0; c < t.naslovi().size(); c++) {
            tekst(sb, adresa(c, 1), t.naslovi().get(c), 1);
        }
        sb.append("</row>");

        int red = 2;
        for (List<Object> r : t.redovi()) {
            sb.append("<row r=\"").append(red).append("\">");
            for (int c = 0; c < r.size(); c++) {
                Object v = r.get(c);
                String a = adresa(c, red);
                if (v == null) continue;
                if (v instanceof Integer || v instanceof Long || v instanceof Short) {
                    sb.append("<c r=\"").append(a).append("\"><v>").append(v).append("</v></c>");
                } else if (v instanceof Double || v instanceof Float || v instanceof BigDecimal) {
                    sb.append("<c r=\"").append(a).append("\" s=\"2\"><v>")
                            .append(((Number) v).doubleValue()).append("</v></c>");
                } else {
                    tekst(sb, a, v.toString(), 0);
                }
            }
            sb.append("</row>");
            red++;
        }
        return sb.append("</sheetData></worksheet>").toString();
    }

    private static void tekst(StringBuilder sb, String adresa, String tekst, int stil) {
        sb.append("<c r=\"").append(adresa).append("\" t=\"inlineStr\"")
                .append(stil != 0 ? " s=\"" + stil + "\"" : "")
                .append("><is><t xml:space=\"preserve\">").append(esc(tekst)).append("</t></is></c>");
    }

    private static String contentTypes(int n) {
        StringBuilder sb = new StringBuilder(XML)
                .append("<Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\">")
                .append("<Default Extension=\"rels\" ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/>")
                .append("<Default Extension=\"xml\" ContentType=\"application/xml\"/>")
                .append("<Override PartName=\"/xl/workbook.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml\"/>")
                .append("<Override PartName=\"/xl/styles.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml\"/>");
        for (int i = 1; i <= n; i++) {
            sb.append("<Override PartName=\"/xl/worksheets/sheet").append(i)
                    .append(".xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml\"/>");
        }
        return sb.append("</Types>").toString();
    }

    private static String workbook(List<Tabela> tabele) {
        StringBuilder sb = new StringBuilder(XML)
                .append("<workbook xmlns=\"").append(NS_MAIN).append("\" xmlns:r=\"").append(NS_REL).append("\"><sheets>");
        for (int i = 0; i < tabele.size(); i++) {
            sb.append("<sheet name=\"").append(esc(imeLista(tabele.get(i).ime()))).append("\" sheetId=\"")
                    .append(i + 1).append("\" r:id=\"rId").append(i + 1).append("\"/>");
        }
        return sb.append("</sheets></workbook>").toString();
    }

    private static String workbookRels(int n) {
        StringBuilder sb = new StringBuilder(XML).append("<Relationships xmlns=\"").append(NS_PKG_REL).append("\">");
        for (int i = 1; i <= n; i++) {
            sb.append("<Relationship Id=\"rId").append(i).append("\" Type=\"").append(NS_REL)
                    .append("/worksheet\" Target=\"worksheets/sheet").append(i).append(".xml\"/>");
        }
        sb.append("<Relationship Id=\"rId").append(n + 1).append("\" Type=\"").append(NS_REL)
                .append("/styles\" Target=\"styles.xml\"/>");
        return sb.append("</Relationships>").toString();
    }

    private static void upisi(ZipOutputStream zip, String ime, String sadrzaj) throws IOException {
        zip.putNextEntry(new ZipEntry(ime));
        zip.write(sadrzaj.getBytes(StandardCharsets.UTF_8));
        zip.closeEntry();
    }

    private static String adresa(int kolona, int red) {
        StringBuilder s = new StringBuilder();
        int n = kolona + 1;
        while (n > 0) {
            int m = (n - 1) % 26;
            s.insert(0, (char) ('A' + m));
            n = (n - 1) / 26;
        }
        return s.append(red).toString();
    }

    private static String imeLista(String ime) {
        String s = ime == null || ime.isBlank() ? "List" : ime.replaceAll("[:\\\\/?*\\[\\]]", "-");
        return s.length() > 31 ? s.substring(0, 31) : s;
    }

    private static String esc(String t) {
        if (t == null) return "";
        StringBuilder sb = new StringBuilder(t.length());
        for (char ch : t.toCharArray()) {
            switch (ch) {
                case '&' -> sb.append("&amp;");
                case '<' -> sb.append("&lt;");
                case '>' -> sb.append("&gt;");
                case '"' -> sb.append("&quot;");
                default -> { if (ch >= 0x20 || ch == '\t' || ch == '\n' || ch == '\r') sb.append(ch); }
            }
        }
        return sb.toString();
    }
}