package util;

import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.HBox;

import java.util.List;

/** Linija s koracima na vrhu svakog ekrana nove prijave. Isti koraci kao na webu. */
public final class KoraciUnosa {

    public static final List<String> KORACI =
            List.of("Student", "Domaćinstvo", "Kriteriji", "Dokumenti", "Pregled");

    private KoraciUnosa() {}

    public static void popuni(HBox stepper, ProgressBar traka, int aktivan) {
        stepper.getChildren().clear();
        stepper.setSpacing(10);
        stepper.setAlignment(Pos.CENTER_LEFT);

        for (int i = 1; i <= KORACI.size(); i++) {
            if (i > 1) {
                Label linija = new Label("—");
                linija.getStyleClass().add("step-line");
                stepper.getChildren().add(linija);
            }

            Label tacka = new Label(String.valueOf(i));
            tacka.getStyleClass().add("step-dot");
            Label naziv = new Label(KORACI.get(i - 1));
            naziv.getStyleClass().add("step-label");

            if (i == aktivan) {
                tacka.getStyleClass().add("step-dot-active");
                naziv.getStyleClass().add("step-label-active");
            }

            HBox korak = new HBox(8, tacka, naziv);
            korak.setAlignment(Pos.CENTER_LEFT);
            stepper.getChildren().add(korak);
        }

        if (traka != null) traka.setProgress((double) aktivan / KORACI.size());
    }
}