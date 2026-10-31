package ba.unze.edom.server.web;

import ba.unze.edom.server.service.javna.ObavjestenjaService;
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
public class ObavjestenjaController {

    private final ObavjestenjaService obavjestenja;

    @GetMapping("/obavjestenja")
    public String stranica(Model model) {
        model.addAttribute("obavjestenja", obavjestenja.sva());
        model.addAttribute("vrste", obavjestenja.vrste());
        return "obavjestenja";
    }

    @GetMapping("/obavjestenja/prilog/{fajl}")
    public ResponseEntity<Resource> prilog(@PathVariable String fajl) {
        var o = obavjestenja.poPrilogu(fajl);
        Path p = obavjestenja.putanja(fajl);
        if (!Files.exists(p)) return ResponseEntity.notFound().build();

        MediaType tip = switch (fajl.substring(fajl.lastIndexOf('.') + 1)) {
            case "pdf" -> MediaType.APPLICATION_PDF;
            case "png" -> MediaType.IMAGE_PNG;
            case "webp" -> MediaType.parseMediaType("image/webp");
            default -> MediaType.IMAGE_JPEG;
        };
        return ResponseEntity.ok()
                .contentType(tip)
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.inline()
                        .filename(o.prilogNaziv() != null ? o.prilogNaziv() : fajl, StandardCharsets.UTF_8).build().toString())
                .header("X-Content-Type-Options", "nosniff")
                .body(new FileSystemResource(p));
    }
}