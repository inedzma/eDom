package controllers;

import dao.SocijalniStatusDAO;
import dao.StudentDAO;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;
import model.SocijalniStatus;
import model.Student;
import util.TextUtil;

public class NoviStudentController {

    @FXML private TextField txtIme;
    @FXML private TextField txtPrezime;
    @FXML private TextField txtIndeks;
    @FXML private ComboBox<String> cmbFakultet;
    @FXML private ComboBox<String> cmbGodina;
    @FXML private TextField txtProsjek;
    @FXML private TextField txtEmail;
    @FXML private TextField txtTelefon;
    @FXML private ComboBox<SocijalniStatus> cmbSocijalniStatus;
    @FXML private TextField txtJMBG;
    @FXML private TextField txtAdresa;
    @FXML private TextField txtRoditelj;

    private String previousView;

    public void setPreviousView(String previousView) {
        this.previousView = previousView;
    }

    private final StudentDAO studentDAO = new StudentDAO();
    private final SocijalniStatusDAO socijalniStatusDAO = new SocijalniStatusDAO();

    @FXML
    public void initialize() {

        cmbFakultet.setItems(FXCollections.observableArrayList(
                "Politehnički fakultet UNZE",
                "Pravni fakultet UNZE",
                "Ekonomski fakultet UNZE",
                "Filozofski fakultet UNZE",
                "Medicinski fakultet UNZE",
                "Mašinski fakultet UNZE",
                "Islamsko pedagoški fakultet UNZE",
                "Fakultet inženjerstva i prirodnih nauka UNZE"
        ));

        cmbGodina.setItems(FXCollections.observableArrayList(
                "1", "2", "3", "4", "5", "6", "Apsolvent", "Postdiplomac"
        ));

        cmbSocijalniStatus.setItems(
                FXCollections.observableArrayList(socijalniStatusDAO.dohvatiSveStatuse())
        );

        cmbSocijalniStatus.setCellFactory(param -> new ListCell<>() {
            @Override
            protected void updateItem(SocijalniStatus item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : item.getNaziv());
            }
        });

        cmbSocijalniStatus.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(SocijalniStatus item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? "" : item.getNaziv());
            }
        });

        txtTelefon.setTextFormatter(new TextFormatter<>(change -> {
            if (change.getText().matches("[0-9+()\\-\\s]*")) {
                return change;
            }
            return null;
        }));
    }

    // ✅ ISPRAVKA: nazad vraća na admin panel + dashboard (u istom stage-u)
    @FXML
    private void onBackClicked() {
        try {
            FXMLLoader adminLoader = new FXMLLoader(
                    getClass().getResource("/views/admin-main-view.fxml")
            );
            Parent root = adminLoader.load();

            AdminController adminController = adminLoader.getController();

            // Ti želiš uvijek dashboard:
            adminController.loadViewPublic("/views/dashboard-view.fxml");

            Stage stage = (Stage) txtIme.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setMaximized(true);
            stage.setResizable(true);

        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Greška", "Ne mogu se vratiti nazad.");
        }
    }

    @FXML
    public void onNextClicked() {

        if (txtIme.getText().isEmpty() ||
                txtPrezime.getText().isEmpty() ||
                txtIndeks.getText().isEmpty() ||
                cmbFakultet.getValue() == null ||
                cmbGodina.getValue() == null ||
                txtProsjek.getText().isEmpty() ||
                cmbSocijalniStatus.getValue() == null ||
                txtAdresa.getText().isEmpty() ||
                txtJMBG.getText().isEmpty() ||
                txtRoditelj.getText().isEmpty()) {

            showAlert("Greška", "Sva obavezna polja moraju biti popunjena.");
            return;
        }

        int godinaStudija;
        double prosjek;

        try {
            prosjek = Double.parseDouble(txtProsjek.getText());

            if (cmbGodina.getValue().equals("Apsolvent")) godinaStudija = 7;
            else if (cmbGodina.getValue().equals("Postdiplomac")) godinaStudija = 8;
            else godinaStudija = Integer.parseInt(cmbGodina.getValue());

            if (godinaStudija == 1) {
                if (prosjek < 1.0 || prosjek > 5.0) {
                    showAlert("Greška", "Prosjek za prvu godinu mora biti između 1.0 i 5.0.");
                    return;
                }
            } else {
                if (prosjek < 6.0 || prosjek > 10.0) {
                    showAlert("Greška", "Prosjek mora biti između 6.0 i 10.0.");
                    return;
                }
            }

        } catch (Exception e) {
            showAlert("Greška", "Neispravan unos godine ili prosjeka.");
            return;
        }

        if (txtJMBG.getText().length() != 13) {
            showAlert("Greška", "JMBG mora imati tačno 13 cifara.");
            return;
        }

        if (studentDAO.postojiJmbg(txtJMBG.getText())) {
            showAlert("Greška", "Student sa tim JMBG-om već postoji.");
            return;
        }

        if (!txtEmail.getText().matches("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$")) {
            showAlert("Greška", "Neispravan format email adrese.");
            return;
        }

        String telefon = txtTelefon.getText().replaceAll("\\D", "");
        if (telefon.length() < 6 || telefon.length() > 15) {
            showAlert("Greška", "Neispravan broj telefona.");
            return;
        }

        Student s = studentDAO.findByBrojIndeksa(txtIndeks.getText());

        if (s == null) {
            s = new Student();
            s.setIme(TextUtil.formatirajIme(txtIme.getText()));
            s.setPrezime(TextUtil.formatirajIme(txtPrezime.getText()));
            s.setImeRoditelja(TextUtil.formatirajIme(txtRoditelj.getText()));
            s.setBrojIndeksa(txtIndeks.getText());
            s.setFakultet(cmbFakultet.getValue());
            s.setGodinaStudija(godinaStudija);
            s.setProsjek(prosjek);
            s.setEmail(txtEmail.getText());
            s.setTelefon(txtTelefon.getText());
            s.setSocijalniStatus(cmbSocijalniStatus.getValue());
            s.setJMBG(txtJMBG.getText());
            s.setAdresa(txtAdresa.getText());

            studentDAO.unesiStudent(s);
            s = studentDAO.findByBrojIndeksa(txtIndeks.getText());
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/nova-prijava.fxml"));
            Parent root = loader.load();

            NovaPrijavaController controller = loader.getController();
            controller.setStudentId(s.getIdStudent());
            controller.setGodinaStudija(godinaStudija);
            controller.setProsjek(prosjek);

            Stage stage = (Stage) txtIme.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setMaximized(true);
            stage.setResizable(true);

        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Greška", "Ne mogu učitati formu za prijavu.");
        }
    }

    private void showAlert(String title, String msg) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(msg);
        a.showAndWait();
    }
}
