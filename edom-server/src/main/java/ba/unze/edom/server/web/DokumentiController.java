package ba.unze.edom.server.web;

import ba.unze.edom.server.service.javna.JavniDokumentiService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

@Controller
@RequiredArgsConstructor
public class DokumentiController {

    private final JavniDokumentiService dokumenti;

    @GetMapping("/dokumenti")
    public String stranica(Model model) {
        model.addAttribute("grupe", dokumenti.grupe());
        model.addAttribute("ukupno", dokumenti.ukupno());
        model.addAttribute("kategorije", dokumenti.kategorije());
        return "dokumenti";
    }

    /** PDF se otvara u pregledniku, Word/Excel se preuzimaju. */
    @GetMapping("/dokumenti/fajl/{fajl}")
    public ResponseEntity<Resource> fajl(@PathVariable String fajl) {
        var d = dokumenti.poFajlu(fajl);
        Path p = dokumenti.putanja(d);
        if (!Files.exists(p)) return ResponseEntity.notFound().build();

        boolean pdf = "pdf".equals(d.ekstenzija());
        MediaType tip = switch (d.ekstenzija()) {
            case "pdf" -> MediaType.APPLICATION_PDF;
            case "doc" -> MediaType.parseMediaType("application/msword");
            case "docx" -> MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.wordprocessingml.document");
            default -> MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        };
        ContentDisposition cd = (pdf ? ContentDisposition.inline() : ContentDisposition.attachment())
                .filename(dokumenti.nazivZaPreuzimanje(d), StandardCharsets.UTF_8)
                .build();

        return ResponseEntity.ok()
                .contentType(tip)
                .header(HttpHeaders.CONTENT_DISPOSITION, cd.toString())
                .header("X-Content-Type-Options", "nosniff")
                .body(new FileSystemResource(p));
    }
}