package util;

import java.util.HashMap;
import java.util.Map;

public class ResetTokenManager {

    // email -> token
    private static Map<String, String> tokenMap = new HashMap<>();

    // dodavanje tokena
    public static void dodajToken(String email, String token) {
        tokenMap.put(email, token);
    }

    // provjera tokena
    public static boolean provjeriToken(String email, String token) {
        if (!tokenMap.containsKey(email)) return false;
        return tokenMap.get(email).equals(token);
    }

    // uklanjanje tokena
    public static void ukloniToken(String email) {
        tokenMap.remove(email);
    }
}
