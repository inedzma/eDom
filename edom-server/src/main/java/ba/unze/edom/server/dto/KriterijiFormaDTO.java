package ba.unze.edom.server.dto;

import java.util.ArrayList;
import java.util.List;

public class KriterijiFormaDTO {
    private List<KriterijIzborDTO> kriteriji = new ArrayList<>();

    public List<KriterijIzborDTO> getKriteriji() { return kriteriji; }
    public void setKriteriji(List<KriterijIzborDTO> k) { kriteriji = k; }
}