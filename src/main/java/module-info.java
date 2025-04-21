module org.example.micro_service_map {
    requires javafx.controls;
    requires javafx.fxml;
    requires com.github.javaparser.core;
    requires org.apache.commons.io;
    requires java.xml;
    requires static lombok;
    requires java.desktop;


    opens org.example.micro_service_map to javafx.fxml;
    exports org.example.micro_service_map;
    exports org.example.micro_service_map.models;
    opens org.example.micro_service_map.models to javafx.fxml;
}