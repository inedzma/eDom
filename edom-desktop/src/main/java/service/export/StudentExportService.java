package service.export;

import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import model.Student;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.util.List;


public class StudentExportService implements ExportService<Student>{

    public void exportData(List<Student> studenti, File file){
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
            for(Student s : studenti) {
                table.addCell(String.valueOf(s.getIdStudent()));
                table.addCell(s.getIme() != null ? s.getIme() : "");
                table.addCell(s.getPrezime() != null ? s.getPrezime() : "");
                table.addCell(s.getBrojIndeksa() != null ? s.getBrojIndeksa() : "");
                table.addCell(s.getFakultet() != null ? s.getFakultet() : "");
                table.addCell(String.valueOf(s.getGodinaStudija()));
                table.addCell(String.valueOf(s.getProsjek()));
                table.addCell(s.getSocijalniStatus() != null ? s.getSocijalniStatus().getNaziv() : "");

            }

            document.add(table);
        } catch (Exception e) {
            e.printStackTrace();
        }finally {
            document.close();
        }

    }
}
