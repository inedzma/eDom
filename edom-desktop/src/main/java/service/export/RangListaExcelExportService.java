package service.export;

import ba.unze.edom.common.dto.rang.KategorijaRanga;
import ba.unze.edom.common.dto.rang.RangListaAdminDTO;
import ba.unze.edom.common.dto.rang.RangRedDTO;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * Rang lista u .xlsx bez dodatnih biblioteka (xlsx = zip s XML fajlovima).
 * Svaka odabrana lista je poseban list; zaglavlje podebljano i zamrznuto; bodovi su brojevi.
 */
public class RangListaExcelExportService {

    private static final String XML = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n";
    private static final String NS_MAIN = "http://schemas.openxmlformats.org/spreadsheetml/2006/main";
    private static final String NS_REL = "http://schemas.openxmlformats.org/officeDocument/2006/relationships";
    private static final String NS_PKG_REL = "http://schemas.openxmlformats.org/package/2006/relationships";

    // stilovi: 0 = obično, 1 = podebljano (zaglavlje), 2 = broj s dvije decimale
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

    /** @return broj prijava izostavljenih zbog neispravnih podataka */
    public int export(RangListaAdminDTO lista, ExportOpcije o, File file) {
        List<String> imenaListova = new ArrayList<>();
        List<String> listovi = new ArrayList<>();

        for (KategorijaRanga k : KategorijaRanga.values()) {
            if (!o.liste().contains(k)) continue;
            List<RangRedDTO> redovi = RangListaHtmlExportService.redovi(lista, k).stream()
                    .filter(r -> r.greska() == null)
                    .filter(r -> !o.samoPrimljeni() || k == KategorijaRanga.LISTA_CEKANJA || r.primljen())
                    .toList();
            imenaListova.add(k.naziv());
            listovi.add(list(redovi, o.kolone()));
        }

        if (o.objedinjeno()) {
            List<RangKolona> saKategorijom = new ArrayList<>(o.kolone());
            if (!saKategorijom.contains(RangKolona.KATEGORIJA)) {
                saKategorijom.add(Math.min(2, saKategorijom.size()), RangKolona.KATEGORIJA);
            }
            List<RangRedDTO> svi = RangListaHtmlExportService.objedinjeno(lista).stream()
                    .filter(r -> r.greska() == null)
                    .toList();
            imenaListova.add("Objedinjeno");
            listovi.add(list(svi, saKategorijom));
        }

        try (ZipOutputStream zip = new ZipOutputStream(new FileOutputStream(file))) {
            upisi(zip, "[Content_Types].xml", contentTypes(listovi.size()));
            upisi(zip, "_rels/.rels", XML
                    + "<Relationships xmlns=\"" + NS_PKG_REL + "\">"
                    + "<Relationship Id=\"rId1\" Type=\"" + NS_REL + "/officeDocument\" Target=\"xl/workbook.xml\"/>"
                    + "</Relationships>");
            upisi(zip, "xl/workbook.xml", workbook(imenaListova));
            upisi(zip, "xl/_rels/workbook.xml.rels", workbookRels(listovi.size()));
            upisi(zip, "xl/styles.xml", STYLES);
            for (int i = 0; i < listovi.size(); i++) {
                upisi(zip, "xl/worksheets/sheet" + (i + 1) + ".xml", listovi.get(i));
            }
        } catch (IOException e) {
            throw new RuntimeException("Greška pri izvozu u Excel.", e);
        }

        return izostavljeno(lista, o);
    }

    // ---------- jedan list ----------

    private String list(List<RangRedDTO> redovi, List<RangKolona> kolone) {
        StringBuilder sb = new StringBuilder(XML)
                .append("<worksheet xmlns=\"").append(NS_MAIN).append("\">")
                // zamrznut prvi red
                .append("<sheetViews><sheetView workbookViewId=\"0\">")
                .append("<pane ySplit=\"1\" topLeftCell=\"A2\" activePane=\"bottomLeft\" state=\"frozen\"/>")
                .append("</sheetView></sheetViews>")
                .append("<cols>");
        for (int c = 0; c < kolone.size(); c++) {
            sb.append("<col min=\"").append(c + 1).append("\" max=\"").append(c + 1)
                    .append("\" width=\"").append(sirina(kolone.get(c))).append("\" customWidth=\"1\"/>");
        }
        sb.append("</cols><sheetData>");

        // zaglavlje
        sb.append("<row r=\"1\">");
        for (int c = 0; c < kolone.size(); c++) {
            tekst(sb, adresa(c, 1), kolone.get(c).naslov(), 1);
        }
        sb.append("</row>");

        // podaci
        int red = 2;
        int rb = 1;
        for (RangRedDTO r : redovi) {
            sb.append("<row r=\"").append(red).append("\">");
            for (int c = 0; c < kolone.size(); c++) {
                RangKolona k = kolone.get(c);
                Number n = k.broj(r, rb);
                if (n == null) {
                    tekst(sb, adresa(c, red), k.vrijednost(r, rb), 0);
                } else if (n instanceof Double d) {
                    sb.append("<c r=\"").append(adresa(c, red)).append("\" s=\"2\"><v>")
                            .append(d).append("</v></c>");
                } else {
                    sb.append("<c r=\"").append(adresa(c, red)).append("\"><v>")
                            .append(n).append("</v></c>");
                }
            }
            sb.append("</row>");
            red++;
            rb++;
        }

        return sb.append("</sheetData></worksheet>").toString();
    }

    private void tekst(StringBuilder sb, String adresa, String tekst, int stil) {
        sb.append("<c r=\"").append(adresa).append("\" t=\"inlineStr\"")
                .append(stil != 0 ? " s=\"" + stil + "\"" : "")
                .append("><is><t xml:space=\"preserve\">").append(esc(tekst)).append("</t></is></c>");
    }

    private int sirina(RangKolona k) {
        return switch (k) {
            case REDNI_BROJ -> 7;
            case SIFRA -> 15;
            case IME -> 34;
            case KATEGORIJA -> 16;
            case STATUS -> 28;
            default -> 12;
        };
    }

    // ---------- okvir xlsx ----------

    private String contentTypes(int brojListova) {
        StringBuilder sb = new StringBuilder(XML)
                .append("<Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\">")
                .append("<Default Extension=\"rels\" ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/>")
                .append("<Default Extension=\"xml\" ContentType=\"application/xml\"/>")
                .append("<Override PartName=\"/xl/workbook.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml\"/>")
                .append("<Override PartName=\"/xl/styles.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.styles+xml\"/>");
        for (int i = 1; i <= brojListova; i++) {
            sb.append("<Override PartName=\"/xl/worksheets/sheet").append(i)
                    .append(".xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml\"/>");
        }
        return sb.append("</Types>").toString();
    }

    private String workbook(List<String> imena) {
        StringBuilder sb = new StringBuilder(XML)
                .append("<workbook xmlns=\"").append(NS_MAIN).append("\" xmlns:r=\"").append(NS_REL).append("\"><sheets>");
        for (int i = 0; i < imena.size(); i++) {
            sb.append("<sheet name=\"").append(esc(imeLista(imena.get(i)))).append("\" sheetId=\"")
                    .append(i + 1).append("\" r:id=\"rId").append(i + 1).append("\"/>");
        }
        return sb.append("</sheets></workbook>").toString();
    }

    private String workbookRels(int brojListova) {
        StringBuilder sb = new StringBuilder(XML).append("<Relationships xmlns=\"").append(NS_PKG_REL).append("\">");
        for (int i = 1; i <= brojListova; i++) {
            sb.append("<Relationship Id=\"rId").append(i).append("\" Type=\"").append(NS_REL)
                    .append("/worksheet\" Target=\"worksheets/sheet").append(i).append(".xml\"/>");
        }
        sb.append("<Relationship Id=\"rId").append(brojListova + 1).append("\" Type=\"").append(NS_REL)
                .append("/styles\" Target=\"styles.xml\"/>");
        return sb.append("</Relationships>").toString();
    }

    // ---------- pomoćne ----------

    private void upisi(ZipOutputStream zip, String ime, String sadrzaj) throws IOException {
        zip.putNextEntry(new ZipEntry(ime));
        zip.write(sadrzaj.getBytes(StandardCharsets.UTF_8));
        zip.closeEntry();
    }

    /** Kolona 0 → A, 25 → Z, 26 → AA … */
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

    /** Excel: ime lista najviše 31 znak, bez : \ / ? * [ ] */
    private static String imeLista(String ime) {
        String s = ime.replaceAll("[:\\\\/?*\\[\\]]", " ");
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

    private int izostavljeno(RangListaAdminDTO l, ExportOpcije o) {
        Set<Integer> ids = new HashSet<>();
        for (KategorijaRanga k : KategorijaRanga.values()) {
            if (o.objedinjeno() || o.liste().contains(k)) {
                RangListaHtmlExportService.redovi(l, k).stream()
                        .filter(r -> r.greska() != null)
                        .forEach(r -> ids.add(r.idPrijava()));
            }
        }
        return ids.size();
    }
}