package dto;

import java.util.LinkedHashMap;
import java.util.Map;

public class RangListaDTO {

    private final Map<String, String> kolone = new LinkedHashMap<>();

    public void addKolona(String naziv, String vrijednost) {
        kolone.put(naziv, vrijednost);
    }

    public Map<String, String> getKolone() {
        return kolone;
    }

}
