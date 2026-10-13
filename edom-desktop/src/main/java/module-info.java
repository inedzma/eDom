module org.example.edom {

    // JavaFX
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.graphics;
    requires javafx.web;

    // Java
    requires java.sql;
    requires java.desktop;
    requires java.prefs;

    // Third-party
    requires org.controlsfx.controls;
    requires com.dlsc.formsfx;
    requires net.synedra.validatorfx;
    requires org.kordamp.ikonli.javafx;
    requires org.kordamp.bootstrapfx.core;
    requires eu.hansolo.tilesfx;
    requires jbcrypt;
    requires jakarta.mail;
    requires com.github.librepdf.openpdf;
    requires openhtmltopdf.core;
    requires openhtmltopdf.pdfbox;


    // JavaFX mora moći instancirati Application
    exports org.example.edom;
    opens org.example.edom to javafx.graphics;

    // FXML controllers
    opens controllers to javafx.fxml;
    opens controllers.DodajDokumenteControllers to javafx.fxml;

    // modeli
    opens model to javafx.base;

    // Ostalo
    opens dao to javafx.fxml;
    opens service to javafx.fxml;

    // 🔥 KLJUČNO: OTVORI FOLDER SA FONTOVIMA 🔥
    opens fonts;

    requires java.net.http;
    requires com.fasterxml.jackson.databind;
    requires ba.unze.edom.common;
    opens api to com.fasterxml.jackson.databind;

    requires com.fasterxml.jackson.datatype.jsr310;
}