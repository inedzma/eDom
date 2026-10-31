package ba.unze.edom.server.service.obavijesti;

import ba.unze.edom.common.dto.opste.ObavijestDTO;

import java.util.List;

/**
 * Jedna vrsta obavijesti za admina. Nova vrsta = nova klasa koja implementira ovo,
 * s @Component i @Order — ObavijestiAdminService je pokupi sama.
 */
public interface IzvorObavijesti {
    List<ObavijestDTO> obavijesti();
}