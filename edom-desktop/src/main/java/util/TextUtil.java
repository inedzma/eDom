package util;

public class TextUtil {

    private TextUtil() {
    }

    public static String formatirajIme(String tekst) {
        if (tekst == null || tekst.isEmpty()) return tekst;

        tekst = tekst.trim().toLowerCase();

        return tekst.substring(0, 1).toUpperCase() + tekst.substring(1);
    }
}
