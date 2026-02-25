module org.mindpower.api_guard {
    requires javafx.controls;
    requires javafx.fxml;
    requires com.github.javaparser.core;
    requires org.apache.commons.io;
    requires java.xml;
    requires static lombok;
    requires java.desktop;
    requires com.brunomnsilva.smartgraph;
    requires info.picocli;
    requires com.fasterxml.jackson.databind;

    opens org.mindpower.api_guard to javafx.fxml, info.picocli;
    exports org.mindpower.api_guard;
    exports org.mindpower.api_guard.models;
    opens org.mindpower.api_guard.models to javafx.fxml;
}