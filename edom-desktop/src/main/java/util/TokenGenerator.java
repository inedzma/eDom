package util;

import java.util.Random;

public class TokenGenerator {

    public static String generate() {
        return String.valueOf(100000 + new Random().nextInt(900000));
    }
}
