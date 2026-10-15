package controllers;

import ba.unze.edom.common.dto.VrstaKriterijaDTO;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import unos.NacrtPrijave;
import util.Ekrani;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

public class NovaPrijavaController {

    @FXML private TextField txtAkGod;
    @FXML private TextField txtClanovi;
    @FXML private TextField txtUdaljenost;
    @FXML private TextArea txtNapomena;
    @FXML private VBox vboxKriteriji;

    private NacrtPrijave nacrt;

    /** Jedan kriterij na ekranu: kvačica i, ako treba, polje za parametar. */
    private record KriterijPolje(VrstaKriterijaDTO vrsta, CheckBox chk, TextField parametar) {}

    private final List<KriterijPolje> polja = new ArrayList<>();

    public void setNacrt(NacrtPrijave n) {
        this.nacrt = n;
        napraviKriterije();
        popuni();
    }

    // ---------- kriteriji iz baze ----------

    private void napraviKriterije() {
        vboxKriteriji.getChildren().clear();
        polja.clear();

        Map<String, List<VrstaKriterijaDTO>> poGrupi = nacrt.sifarnici.kriteriji().stream()
                .collect(Collectors.groupingBy(VrstaKriterijaDTO::grupa,
                        LinkedHashMap::new, Collectors.toList()));

        poGrupi.forEach((grupa, vrste) -> {
            Label naslov = new Label(nazivGrupe(grupa));
            naslov.getStyleClass().add("field-label");
            vboxKriteriji.getChildren().add(naslov);

            for (VrstaKriterijaDTO v : vrste) {
                CheckBox chk = new CheckBox(v.naziv());
                chk.getStyleClass().add("checkbox");
                chk.setWrapText(true);
                chk.setMaxWidth(Double.MAX_VALUE);

                VBox red = new VBox(6, chk);

                TextField param = null;
                if (v.traziParametar()) {
                    param = new TextField();
                    param.setPromptText(v.nazivParametra());
                    param.getStyleClass().add("field");
                    param.setMaxWidth(320);
                    VBox.setMargin(param, new Insets(0, 0, 0, 34));   // uvučeno ispod kvačice
                    param.visibleProperty().bind(chk.selectedProperty());
                    param.managedProperty().bind(chk.selectedProperty());
                    red.getChildren().add(param);
                }

                polja.add(new KriterijPolje(v, chk, param));
                vboxKriteriji.getChildren().add(red);
            }
        });
    }

    private String nazivGrupe(String grupa) {
        if ("BRANIOCI".equalsIgnoreCase(grupa)) return "Boračke i socijalne kategorije (boduje se samo najveća)";
        if ("DODATNI".equalsIgnoreCase(grupa)) return "Ostali kriteriji (sabiraju se)";
        return grupa;
    }

    private void popuni() {
        txtAkGod.setText(String.valueOf(
                nacrt.akademskaGodina != null ? nacrt.akademskaGodina : LocalDate.now().getYear()));
        if (nacrt.brojClanova != null) txtClanovi.setText(String.valueOf(nacrt.brojClanova));
        if (nacrt.udaljenostKm != null) txtUdaljenost.setText(nacrt.udaljenostKm.toPlainString());
        if (nacrt.napomena != null) txtNapomena.setText(nacrt.napomena);

        for (KriterijPolje p : polja) {
            String sifra = p.vrsta().sifra();
            if (nacrt.kriteriji.containsKey(sifra)) {
                p.chk().setSelected(true);
                BigDecimal param = nacrt.kriteriji.get(sifra);
                if (p.parametar() != null && param != null) p.parametar().setText(param.toPlainString());
            }
        }
    }

    // ---------- navigacija ----------

    @FXML
    private void onSaveClicked() {
        Integer akGod = cijeli(txtAkGod);
        Integer clanovi = cijeli(txtClanovi);
        BigDecimal udaljenost = decimalni(txtUdaljenost);

        if (akGod == null || clanovi == null || udaljenost == null) {
            alert("Akademska godina, broj članova i udaljenost su obavezni i moraju biti brojevi.");
            return;
        }
        if (akGod < LocalDate.now().getYear()) {
            alert("Akademska godina ne može biti manja od tekuće.");
            return;
        }
        if (clanovi < 1) {
            alert("Domaćinstvo ima najmanje jednog člana (studenta).");
            return;
        }

        Map<String, BigDecimal> izabrani = new LinkedHashMap<>();
        for (KriterijPolje p : polja) {
            if (!p.chk().isSelected()) continue;

            BigDecimal param = null;
            if (p.parametar() != null) {
                param = decimalni(p.parametar());
                if (param == null) {
                    alert("Za kriterij '" + p.vrsta().naziv() + "' unesite "
                            + p.vrsta().nazivParametra().toLowerCase() + ".");
                    return;
                }
            }
            izabrani.put(p.vrsta().sifra(), param);
        }

        nacrt.akademskaGodina = akGod;
        nacrt.brojClanova = clanovi;
        nacrt.udaljenostKm = udaljenost;
        nacrt.napomena = txtNapomena.getText();
        nacrt.kriteriji.clear();
        nacrt.kriteriji.putAll(izabrani);

        Ekrani.<DodajDokumenteController>zamijeni(txtAkGod, "/views/dodaj-dokumente.fxml",
                c -> c.setNacrt(nacrt));
    }

    @FXML
    private void onBackClicked() {
        Ekrani.<NoviStudentController>zamijeni(txtAkGod, "/views/novi-student.fxml",
                c -> c.setNacrt(nacrt));
    }

    // ---------- pomoćne ----------

    private Integer cijeli(TextField t) {
        try {
            return Integer.parseInt(t.getText().trim());
        } catch (Exception e) {
            return null;
        }
    }

    private BigDecimal decimalni(TextField t) {
        try {
            return new BigDecimal(t.getText().trim().replace(',', '.'));
        } catch (Exception e) {
            return null;
        }
    }

    private void alert(String poruka) {
        Alert a = new Alert(Alert.AlertType.INFORMATION, poruka);
        a.setHeaderText(null);
        a.showAndWait();
    }
}