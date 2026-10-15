module org.example.edom {

    // JavaFX
    requires javafx.controls;
    requires javafx.fxml;
    requires javafx.graphics;
    requires javafx.web;

    // Java
    requires java.desktop;
    requires java.prefs;
    requires java.net.http;

    // UI biblioteke
    requires org.controlsfx.controls;
    requires com.dlsc.formsfx;
    requires net.synedra.validatorfx;
    requires org.kordamp.ikonli.javafx;
    requires org.kordamp.bootstrapfx.core;
    requires eu.hansolo.tilesfx;

    // PDF
    requires com.github.librepdf.openpdf;
    requires openhtmltopdf.core;
    requires openhtmltopdf.pdfbox;

    // komunikacija sa serverom
    requires com.fasterxml.jackson.databind;
    requires com.fasterxml.jackson.datatype.jsr310;
    requires ba.unze.edom.common;

    exports org.example.edom;
    opens org.example.edom to javafx.graphics;

    opens controllers to javafx.fxml;
    opens service to javafx.fxml;
    opens api to com.fasterxml.jackson.databind;

    opens fonts;
}