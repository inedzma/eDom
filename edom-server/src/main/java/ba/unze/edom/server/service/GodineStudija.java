package ba.unze.edom.server.service;

/** Godina studija se u bazi cuva kao broj. 7 i 8 su posebne vrijednosti. */
public final class GodineStudija {

    public static final int PRVA         = 1;
    public static final int APSOLVENT    = 7;
    public static final int POSTDIPLOMAC = 8;

    public static String naziv(int godina) {
        return switch (godina) {
            case APSOLVENT    -> "Apsolvent";
            case POSTDIPLOMAC -> "Postdiplomac";
            default           -> godina + ". godina";
        };
    }

    public static final int MIN = 1;
    public static final int MAX = 8;

    public static boolean validna(Integer godina) {
        return godina != null && godina >= MIN && godina <= MAX;
    }

    private GodineStudija() {}
}