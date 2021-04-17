package gameserver;

import javafx.geometry.Insets;
import javafx.scene.control.ListView;
import javafx.scene.control.TextArea;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.RowConstraints;

public class ServerUI extends GridPane {

    protected final ColumnConstraints columnConstraints;
    protected final RowConstraints rowConstraints;
    protected final BorderPane borderPane;
    protected final TextArea txtArea;
    protected final ListView lvClients;

    public Server server;

    public ServerUI() {

        columnConstraints = new ColumnConstraints();
        rowConstraints = new RowConstraints();
        borderPane = new BorderPane();
        txtArea = new TextArea();
        lvClients = new ListView();

        setPrefHeight(500);
        setPrefWidth(500);

        GridPane.setHgrow(borderPane, javafx.scene.layout.Priority.ALWAYS);
        GridPane.setVgrow(borderPane, javafx.scene.layout.Priority.ALWAYS);

        GridPane.setHgrow(txtArea, javafx.scene.layout.Priority.ALWAYS);
        GridPane.setVgrow(txtArea, javafx.scene.layout.Priority.ALWAYS);
        txtArea.setDisable(true);
        txtArea.setEditable(false);
        txtArea.setId("chatArea");
        BorderPane.setMargin(txtArea, new Insets(5.0));
        borderPane.setCenter(txtArea);

        BorderPane.setAlignment(lvClients, javafx.geometry.Pos.CENTER);
        lvClients.setMaxWidth(200.0);
        lvClients.setMinWidth(200.0);
        lvClients.setPrefHeight(200.0);
        lvClients.setPrefWidth(200.0);
        BorderPane.setMargin(lvClients, new Insets(5.0));
        borderPane.setRight(lvClients);

        getColumnConstraints().add(columnConstraints);
        getRowConstraints().add(rowConstraints);
        getChildren().add(borderPane);

        server = new Server(txtArea, lvClients);
    }
}
