package controllers;

import ba.unze.edom.common.dto.bodovanje.VrstaKriterijaDTO;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import unos.NacrtPrijave;
import util.Ekrani;
import util.KoraciUnosa;

import java.math.BigDecimal;
import java.util.*;
import java.util.stream.Collectors;

/** Korak 3: dodatni kriteriji, s parametrom ili periodom (OS RBiH). */
public class KriterijiUnosController {

    @FXML private HBox stepper;
    @FXML private ProgressBar traka;
    @FXML private VBox vboxKriteriji;

    private NacrtPrijave nacrt;

    private record KriterijPolje(VrstaKriterijaDTO vrsta, CheckBox chk, TextField parametar,
                                 DatePicker od, DatePicker doDatuma) {}

    private final List<KriterijPolje> polja = new ArrayList<>();

    @FXML
    public void initialize() {
        KoraciUnosa.popuni(stepper, traka, 3);
    }

    public void setNacrt(NacrtPrijave n) {
        this.nacrt = n;
        napraviKriterije();
        popuni();
    }

    private void napraviKriterije() {
        vboxKriteriji.getChildren().clear();
        polja.clear();

        Map<String, List<VrstaKriterijaDTO>> poGrupi = nacrt.sifarnici.kriteriji().stream()
                .collect(Collectors.groupingBy(v -> v.grupa() != null ? v.grupa() : "Ostalo",
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
                    VBox.setMargin(param, new Insets(0, 0, 0, 34));
                    param.visibleProperty().bind(chk.selectedProperty());
                    param.managedProperty().bind(chk.selectedProperty());
                    red.getChildren().add(param);
                }

                DatePicker od = null, doDatuma = null;
                if (v.traziPeriod()) {
                    od = new DatePicker();
                    od.setPromptText("Od");
                    doDatuma = new DatePicker();
                    doDatuma.setPromptText("Do");
                    HBox period = new HBox(10, new Label("Od:"), od, new Label("Do:"), doDatuma);
                    period.setAlignment(Pos.CENTER_LEFT);
                    VBox.setMargin(period, new Insets(0, 0, 0, 34));
                    period.visibleProperty().bind(chk.selectedProperty());
                    period.managedProperty().bind(chk.selectedProperty());
                    red.getChildren().add(period);
                }

                polja.add(new KriterijPolje(v, chk, param, od, doDatuma));
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
        for (KriterijPolje p : polja) {
            String sifra = p.vrsta().sifra();
            if (!nacrt.kriteriji.containsKey(sifra)) continue;

            p.chk().setSelected(true);
            BigDecimal param = nacrt.kriteriji.get(sifra);
            if (p.parametar() != null && param != null) p.parametar().setText(param.toPlainString());

            NacrtPrijave.Period per = nacrt.periodi.get(sifra);
            if (p.od() != null && per != null) {
                p.od().setValue(per.od());
                p.doDatuma().setValue(per.doDatuma());
            }
        }
    }

    // ---------- navigacija ----------

    @FXML
    private void onDalje() {
        Map<String, BigDecimal> izabrani = new LinkedHashMap<>();
        Map<String, NacrtPrijave.Period> periodi = new HashMap<>();

        for (KriterijPolje p : polja) {
            if (!p.chk().isSelected()) continue;
            VrstaKriterijaDTO v = p.vrsta();

            BigDecimal param = null;
            if (p.parametar() != null) {
                param = decimalni(p.parametar());
                if (param == null) {
                    alert("Za kriterij '" + v.naziv() + "' unesite " + v.nazivParametra().toLowerCase() + ".");
                    return;
                }
            }

            if (p.od() != null) {
                if (p.od().getValue() == null || p.doDatuma().getValue() == null) {
                    alert("Za kriterij '" + v.naziv() + "' unesite datum od i datum do.");
                    return;
                }
                if (p.od().getValue().isAfter(p.doDatuma().getValue())) {
                    alert("Za kriterij '" + v.naziv() + "' datum od ne može biti poslije datuma do.");
                    return;
                }
                periodi.put(v.sifra(), new NacrtPrijave.Period(p.od().getValue(), p.doDatuma().getValue()));
            }

            izabrani.put(v.sifra(), param);
        }

        sacuvaj(izabrani, periodi);
        Ekrani.<DodajDokumenteController>zamijeni(vboxKriteriji, "/views/dodaj-dokumente.fxml",
                c -> c.setNacrt(nacrt));
    }

    @FXML
    private void onNazad() {
        Ekrani.<NovaPrijavaController>zamijeni(vboxKriteriji, "/views/nova-prijava.fxml",
                c -> c.setNacrt(nacrt));
    }

    private void sacuvaj(Map<String, BigDecimal> izabrani, Map<String, NacrtPrijave.Period> periodi) {
        nacrt.kriteriji.clear();
        nacrt.kriteriji.putAll(izabrani);
        nacrt.periodi.clear();
        nacrt.periodi.putAll(periodi);
        nacrt.provjereniKriteriji.retainAll(izabrani.keySet());
    }

    // ---------- pomoćne ----------

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