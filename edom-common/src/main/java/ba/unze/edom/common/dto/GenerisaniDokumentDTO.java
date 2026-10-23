package ba.unze.edom.common.dto;

/** Generisan dokument (ugovor, revers) kao Base64, s predloženim imenom fajla. */
public record GenerisaniDokumentDTO(String naziv, String b64) {}