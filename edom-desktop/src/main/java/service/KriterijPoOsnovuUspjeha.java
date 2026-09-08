package service;

import dao.PrijavaDAO;

public class KriterijPoOsnovuUspjeha {

    public double izracunajBodove(int godinaStudija, double prosjek, int polozeniIspiti){
        double bodovi = prosjek*3.5;
        bodovi += polozeniIspiti*1.8;

        switch(godinaStudija){
            case 2: bodovi += 3; break;
            case 3: bodovi += 5; break;
            case 4: bodovi += 7; break;
            case 5: bodovi += 9; break;
            case 6: bodovi += 10; break;
            case 7: bodovi += 12; break;
            case 8: bodovi += 10; break;
        }
        return bodovi;
    }

    public double izracunajBodoveBrucosi(double prosjek){
        return prosjek*9 + 1;
    }

}
