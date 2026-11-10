package service;

import javafx.stage.FileChooser;
import javafx.stage.Window;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Base64;

public class PdfService {


    // Metoda za prikaz PDF-a iz Base64 stringa
    public static void prikaziPdf(String base64, String naziv) {
        if (base64 == null || base64.isEmpty()) {
            System.out.println("PDF dokument ne postoji.");
            return;
        }

        try {
            byte[] pdfBytes = Base64.getDecoder().decode(base64);

            // ✅ Sanitizuj naziv fajla (Windows-safe)
            String safeNaziv = sanitizeFileName(naziv);

            // ✅ napravi temp fajl
            File tempFile = File.createTempFile(safeNaziv + "_", ".pdf");

            Files.write(tempFile.toPath(), pdfBytes);
            tempFile.deleteOnExit();

            if (java.awt.Desktop.isDesktopSupported()) {
                java.awt.Desktop.getDesktop().open(tempFile);
            } else {
                System.out.println("Otvaranje PDF-a nije podržano na ovom sistemu.");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ✅ uklanja sve nedozvoljene znakove u Windows fajlovima
    private static String sanitizeFileName(String input) {
        if (input == null || input.isBlank()) return "dokument";

        // zamijeni sve ilegalne znakove: \ / : * ? " < > |
        String safe = input.replaceAll("[\\\\/:*?\"<>|]", "_");

        // ukloni višak razmaka
        safe = safe.replaceAll("\\s+", "_").trim();

        // prevent preduga imena (Windows zna praviti problem)
        if (safe.length() > 60) {
            safe = safe.substring(0, 60);
        }

        return safe;
    }
}
