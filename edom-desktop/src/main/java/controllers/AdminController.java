package controllers;

import api.ObavijestiApi;
import ba.unze.edom.common.dto.ObavijestDTO;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.IOException;
import java.util.*;
import java.util.prefs.Preferences;
import java.util.stream.Collectors;

public class AdminController {

    private static final String PREF_VIDJENE = "obavijesti.vidjene";

    @FXML private StackPane contentArea;
    @FXML private ToggleGroup navGroup;

    // obavijesti
    private final Button btnObavijesti = new Button("🔔 Obavijesti");
    private final Preferences prefs = Preferences.userNodeForPackage(AdminController.class);
    private List<ObavijestDTO> obavijesti = List.of();
    private boolean panelOtvoren;
    private Timeline osvjezavanje;

    @FXML
    public void initialize() {
        loadView("/views/dashboard-view.fxml");
        initObavijesti();
    }

    @FXML
    public void showPregledSistema(ActionEvent event) {
        loadView("/views/dashboard-view.fxml");
    }

    @FXML
    public void showPrijave(ActionEvent event) {
        loadView("/views/prijave.fxml");
    }

    @FXML
    public void showStudenti(ActionEvent event) {
        loadView("/views/studenti.fxml");
    }

    @FXML
    public void showSobe(ActionEvent event) {
        loadView("/views/sobe.fxml");
    }

    @FXML
    public void onMojProfil(ActionEvent event) {
        loadView("/views/moj-profil.fxml");
    }

    @FXML
    public void showRangLista(ActionEvent actionEvent) {
        loadView("/views/rang-lista.fxml");
    }

    @FXML
    public void showPostavke(ActionEvent event) {
        panelOtvoren = false;
        PostavkePanel panel = new PostavkePanel();
        var css = getClass().getResource("/css/app.css");
        if (css != null) panel.getStylesheets().add(css.toExternalForm());
        contentArea.getChildren().setAll(panel);
    }

    public void loadViewPublic(String fxmlPath) {
        loadView(fxmlPath);
    }

    private void loadView(String fxmlPath) {
        if (contentArea == null) return;
        panelOtvoren = false;

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource(fxmlPath));
            Parent view = loader.load();

            var globalCss = getClass().getResource("/css/app.css");
            if (globalCss != null && !view.getStylesheets().contains(globalCss.toExternalForm())) {
                view.getStylesheets().add(globalCss.toExternalForm());
            }

            if ("/views/prijave.fxml".equals(fxmlPath)) {
                var prijaveCss = getClass().getResource("/css/prijave.css");
                if (prijaveCss != null && !view.getStylesheets().contains(prijaveCss.toExternalForm())) {
                    view.getStylesheets().add(prijaveCss.toExternalForm());
                }
            }

            Object controller = loader.getController();
            if (controller instanceof DashboardController) {
                ((DashboardController) controller).setAdminController(this);
            }

            contentArea.getChildren().setAll(view);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // ---------- obavijesti ----------

    private void initObavijesti() {
        btnObavijesti.setMaxWidth(Double.MAX_VALUE);
        btnObavijesti.getStyleClass().addAll("nav-btn", "obavijesti-btn");
        btnObavijesti.setOnAction(e -> prikaziObavijesti());

        // ubaci u lijevi meni, odmah ispod naslova i separatora
        VBox meni = (VBox) ((BorderPane) contentArea.getParent()).getLeft();
        meni.getChildren().add(2, btnObavijesti);

        osvjeziObavijesti();
        osvjezavanje = new Timeline(new KeyFrame(Duration.seconds(60), e -> osvjeziObavijesti()));
        osvjezavanje.setCycleCount(Timeline.INDEFINITE);
        osvjezavanje.play();
    }

    private void prikaziObavijesti() {
        if (navGroup != null && navGroup.getSelectedToggle() != null) {
            navGroup.getSelectedToggle().setSelected(false);
        }
        panelOtvoren = true;
        contentArea.getChildren().setAll(new Label("Učitavanje obavijesti…"));
        osvjeziObavijesti();   // kad stigne, prikaže panel jer je panelOtvoren
    }

    private void osvjeziObavijesti() {
        Task<List<ObavijestDTO>> task = new Task<>() {
            @Override
            protected List<ObavijestDTO> call() {
                return ObavijestiApi.sve();
            }
        };
        task.setOnSucceeded(e -> {
            List<ObavijestDTO> nove = task.getValue();
            boolean promjena = !kljucevi(nove).equals(kljucevi(obavijesti));
            obavijesti = nove;
            if (panelOtvoren && (promjena || !(contentArea.getChildren().getFirst() instanceof ObavijestiPanel))) {
                prikaziPanel();
            }
            azurirajDugme();
        });
        // osvježava se svake minute — greške samo u konzolu, bez prozora
        task.setOnFailed(e -> System.err.println("Obavijesti nisu učitane: " + task.getException().getMessage()));

        Thread t = new Thread(task);
        t.setDaemon(true);
        t.start();
    }

    private void prikaziPanel() {
        contentArea.getChildren().setAll(new ObavijestiPanel(obavijesti, vidjene(), this::otvoriObavijest));
        // sve što je sada prikazano računa se kao viđeno
        prefs.put(PREF_VIDJENE, String.join(",", kljucevi(obavijesti)));
    }

    private void azurirajDugme() {
        Set<String> vidjene = vidjene();
        long nepregledane = obavijesti.stream().filter(o -> !vidjene.contains(o.kljuc())).count();

        btnObavijesti.setText("🔔 Obavijesti" + (obavijesti.isEmpty() ? "" : " (" + obavijesti.size() + ")"));
        btnObavijesti.getStyleClass().remove("ima-novih");
        if (nepregledane > 0) btnObavijesti.getStyleClass().add("ima-novih");
        btnObavijesti.setTooltip(new Tooltip(nepregledane > 0
                ? nepregledane + " nepregledanih obavijesti"
                : "Nema novih obavijesti"));
    }

    private void otvoriObavijest(ObavijestDTO o) {
        switch (o.vrsta()) {
            case "NOVA_PRIJAVA" -> otvoriDetalje(o.idPrijava());
            case "BEZ_SOBE", "ZAHTJEV_SOBA" -> loadView("/views/sobe.fxml");
            default -> { if (o.idPrijava() != null) otvoriDetalje(o.idPrijava()); }
        }
    }

    private void otvoriDetalje(Integer idPrijave) {
        if (idPrijave == null) return;
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/detalji-prijave.fxml"));
            Scene scene = new Scene(loader.load());
            DetaljiPrijaveController controller = loader.getController();
            controller.ucitaj(idPrijave);

            Stage stage = new Stage();
            stage.setTitle("Detalji prijave");
            stage.setScene(scene);
            stage.setOnHidden(e -> osvjeziObavijesti());   // obrađena prijava nestaje iz obavijesti
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private Set<String> vidjene() {
        String s = prefs.get(PREF_VIDJENE, "");
        return s.isBlank() ? Set.of() : new HashSet<>(Arrays.asList(s.split(",")));
    }

    private static Set<String> kljucevi(List<ObavijestDTO> lista) {
        return lista.stream().map(ObavijestDTO::kljuc).collect(Collectors.toCollection(LinkedHashSet::new));
    }

    // ---------- odjava ----------

    @FXML
    public void onLogout(ActionEvent event) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Odjava");
        alert.setHeaderText(null);
        alert.setContentText("Da li ste sigurni da se želite odjaviti?");

        if (alert.showAndWait().orElse(ButtonType.CANCEL) != ButtonType.OK) {
            return;
        }

        if (osvjezavanje != null) osvjezavanje.stop();

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/login-view.fxml"));
            Parent root = loader.load();

            Stage stage = (Stage) contentArea.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.centerOnScreen();

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}