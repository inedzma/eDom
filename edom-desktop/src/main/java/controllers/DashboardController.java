package controllers;

import dao.StudentDAO;
import dao.PrijavaDAO;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.chart.PieChart;
import javafx.scene.control.Label;
import javafx.stage.Stage;

public class DashboardController {

    private AdminController adminController;


    @FXML private Label lblStudenti;
    @FXML private Label lblPrijave;
    @FXML private PieChart pieStatusChart;
    @FXML private Label lblAdminIme;

    @FXML private Label lblNaPregledu;
    @FXML private Label lblOdobrene;
    @FXML private Label lblOdbijene;
    @FXML private Label lblBezBodova;
    @FXML private Label lblZakljucene;

    private final StudentDAO studentDAO = new StudentDAO();
    private final PrijavaDAO prijavaDAO = new PrijavaDAO();

    @FXML
    public void initialize() {

        try {
            lblStudenti.setText(String.valueOf(studentDAO.countStudents()));
        } catch (Exception e) {
            lblStudenti.setText("0");
            e.printStackTrace();
        }


        try {
            lblPrijave.setText(String.valueOf(prijavaDAO.countPrijave()));
        } catch (Exception e) {
            lblPrijave.setText("0");
            e.printStackTrace();
        }

        initStatusStatistiku();
        initPieChartStatusa();
    }

    private void initPieChartStatusa() {
        try {
            ObservableList<PieChart.Data> data = FXCollections.observableArrayList(
                    new PieChart.Data("Na pregledu", prijavaDAO.countPrijaveByStatusId(1)),
                    new PieChart.Data("Odobrene", prijavaDAO.countPrijaveByStatusId(4)),
                    new PieChart.Data("Odbijene", prijavaDAO.countPrijaveByStatusId(5)),
                    new PieChart.Data("Bez bodova", prijavaDAO.countPrijaveBezBodova()),
                    new PieChart.Data("Zaključene", prijavaDAO.countPrijaveByStatusId(3))
            );

            pieStatusChart.setData(data);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void initStatusStatistiku() {
        try {
            lblNaPregledu.setText(String.valueOf(prijavaDAO.countPrijaveByStatusId(1)));
            lblOdobrene.setText(String.valueOf(prijavaDAO.countPrijaveByStatusId(4)));
            lblOdbijene.setText(String.valueOf(prijavaDAO.countPrijaveByStatusId(5)));
            lblBezBodova.setText(String.valueOf(prijavaDAO.countPrijaveBezBodova()));
            lblZakljucene.setText(String.valueOf(prijavaDAO.countPrijaveByStatusId(3)));

        } catch (Exception e) {
            e.printStackTrace();
            lblNaPregledu.setText("0");
            lblOdobrene.setText("0");
            lblOdbijene.setText("0");
            lblBezBodova.setText("0");
            lblZakljucene.setText("0");
        }
    }

    public void setAdminController(AdminController adminController) {
        this.adminController = adminController;
    }

    @FXML
    private void goPrijave(ActionEvent e) {
        if (adminController != null) {
            adminController.showPrijave(null);
        }
    }

    @FXML
    private void goStudenti(ActionEvent e) {
        if (adminController != null) {
            adminController.showStudenti(null);
        }
    }


    @FXML
    private void onNovaPrijava() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/views/novi-student.fxml")
            );
            Scene newScene = new Scene(loader.load());

            NoviStudentController controller = loader.getController();
            controller.setPreviousView("dashboard");

            Stage currentStage = (Stage) pieStatusChart.getScene().getWindow();
            currentStage.setScene(newScene);

            currentStage.setMaximized(true);
            currentStage.setResizable(true);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
