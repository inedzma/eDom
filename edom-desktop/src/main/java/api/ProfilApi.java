package api;

import ba.unze.edom.common.dto.korisnik.KorisnikIzmjenaDTO;
import ba.unze.edom.common.dto.korisnik.KorisnikProfilDTO;
import ba.unze.edom.common.dto.korisnik.PromjenaLozinkeDTO;

public final class ProfilApi {

    private ProfilApi() {}

    public static KorisnikProfilDTO dohvati() {
        return ApiClient.get("/api/profil", KorisnikProfilDTO.class);
    }

    public static KorisnikProfilDTO izmijeni(String ime, String prezime) {
        return ApiClient.put("/api/profil", new KorisnikIzmjenaDTO(ime, prezime), KorisnikProfilDTO.class);
    }

    public static void promijeniLozinku(String stara, String nova) {
        ApiClient.put("/api/profil/lozinka", new PromjenaLozinkeDTO(stara, nova), Void.class);
    }
}