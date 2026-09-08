package service;

public class KriterijPoOsnovuUdaljenosti {

    public double izracunajBodove(double udaljenostKm) {
        if(udaljenostKm<50) return 0;
        else if(udaljenostKm<80) return 4;
        else if(udaljenostKm<120) return 8;
        else if(udaljenostKm<160) return 12;
        else return 15;
    }
}
