module com.serverclient {
    requires javafx.controls;
    requires javafx.fxml;
    requires java.desktop;


    opens com.serverclient to javafx.fxml;
    exports com.serverclient;
}