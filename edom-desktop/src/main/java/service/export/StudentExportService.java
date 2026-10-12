package service.export;

import ba.unze.edom.common.dto.StudentListaDTO;
import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.List;


public class StudentExportService implements ExportService<StudentListaDTO>{

    public void exportData(List<StudentListaDTO> studenti, File file){
        Document document = new Document(PageSize.A4);

        try {
            PdfWriter.getInstance(document, new FileOutputStream(file));
            document.open();

            Paragraph title = new Paragraph("Lista studenata",
                    FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, Font.BOLD));
            title.setAlignment(Element.ALIGN_CENTER);
            title.setSpacingAfter(20);
            document.add(title);

            PdfPTable table = new PdfPTable(8);
            table.setWidthPercentage(100);

            String[] headers = {"ID", "Ime", "Prezime", "Broj Indeksa", "Fakultet", "Godina Studija", "Prosjek", "Status"};
            for (String header : headers) {
                table.addCell(new Phrase(header, FontFactory.getFont(FontFactory.HELVETICA_BOLD)));
            }
            for (StudentListaDTO s : studenti) {
                table.addCell(String.valueOf(s.idStudent()));
                table.addCell(s.ime() != null ? s.ime() : "");
                table.addCell(s.prezime() != null ? s.prezime() : "");
                table.addCell(s.brojIndeksa() != null ? s.brojIndeksa() : "");
                table.addCell(s.fakultet() != null ? s.fakultet() : "");
                table.addCell(s.godinaStudija() != null ? String.valueOf(s.godinaStudija()) : "");
                table.addCell(s.prosjek() != null ? String.format("%.2f", s.prosjek()) : "-");
                table.addCell(s.socijalniStatus() != null ? s.socijalniStatus().naziv() : "");
            }

            document.add(table);
        } catch (Exception e) {
            e.printStackTrace();
        }finally {
            document.close();
        }

    }
}
