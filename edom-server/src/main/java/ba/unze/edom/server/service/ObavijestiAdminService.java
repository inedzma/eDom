package ba.unze.edom.server.service;

import ba.unze.edom.common.dto.ObavijestDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

/** Skuplja obavijesti iz svih izvora (redoslijed po @Order). */
@Service
@RequiredArgsConstructor
public class ObavijestiAdminService {

    private final List<IzvorObavijesti> izvori;   // Spring ubaci sve implementacije

    @Transactional(readOnly = true)
    public List<ObavijestDTO> sve() {
        List<ObavijestDTO> out = new ArrayList<>();
        for (IzvorObavijesti izvor : izvori) {
            out.addAll(izvor.obavijesti());
        }
        return out;
    }
}