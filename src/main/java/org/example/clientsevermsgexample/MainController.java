package org.example.clientsevermsgexample;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.*;
import java.util.ResourceBundle;

public class MainController implements Initializable {

    @FXML private ComboBox<String> dropdownPort;
    @FXML private TextArea resultArea;
    @FXML private TextField urlName;

    private DataInputStream serverInput;
    private DataOutputStream serverOutput;
    private DataInputStream clientInput;
    private DataOutputStream clientOutput;
    private Socket clientSocket;
    private Socket serverSocketInstance;

    private TextArea serverChatArea;
    private TextArea clientChatArea;
    private TextField msgText;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        dropdownPort.getItems().addAll("7", "13", "21", "23", "71", "80", "119", "161");
    }

    @FXML
    void checkConnection(ActionEvent event) {
        String host = urlName.getText();
        int port = Integer.parseInt(dropdownPort.getValue());
        try (Socket sock = new Socket(host, port)) {
            resultArea.appendText(host + " listening on port " + port + "\n");
        } catch (UnknownHostException e) {
            resultArea.appendText("Unknown Host: " + e.getMessage() + "\n");
        } catch (IOException e) {
            resultArea.appendText(host + " not listening on port " + port + "\n");
        }
    }

    @FXML
    void clearBtn(ActionEvent event) {
        resultArea.setText("");
        urlName.setText("");
    }

    @FXML
    void startUser1Client() {
        startClient(null);
    }

    @FXML
    void startUser2Server() {
        startServer(null);
    }
    @FXML
    void startServer(ActionEvent event) {
        Stage stage = new Stage();
        Group root = new Group();

        Label title = new Label("User 2");
        title.setLayoutX(100);
        title.setLayoutY(20);

        serverChatArea = new TextArea();
        serverChatArea.setLayoutX(50);
        serverChatArea.setLayoutY(60);
        serverChatArea.setPrefSize(400, 160);
        serverChatArea.setEditable(false);

        TextField serverMsgField = new TextField();
        serverMsgField.setLayoutX(50);
        serverMsgField.setLayoutY(230);
        serverMsgField.setPrefWidth(300);

        Button sendToClientBtn = new Button("Send");
        sendToClientBtn.setLayoutX(370);
        sendToClientBtn.setLayoutY(230);
        sendToClientBtn.setOnAction(e -> {
            try {
                String msg = serverMsgField.getText();
                if (!msg.isEmpty()) {
                    clientOutput.writeUTF(msg);
                    updateServerUI("Me: " + msg);
                    serverMsgField.clear();
                }
            } catch (IOException ex) {
                updateServerUI("Error sending message: " + ex.getMessage());
            }
        });

        root.getChildren().addAll(title, serverChatArea, serverMsgField, sendToClientBtn);
        Scene scene = new Scene(root, 500, 300);
        stage.setScene(scene);
        stage.setTitle("Server");
        stage.show();

        new Thread(this::runServer).start();
    }


    private void runServer() {
        try (ServerSocket serverSocket = new ServerSocket(6666)) {
            updateServerUI("Server started. Waiting for client...");

            serverSocketInstance = serverSocket.accept();
            updateServerUI("Client connected.");

            clientInput = new DataInputStream(serverSocketInstance.getInputStream());
            clientOutput = new DataOutputStream(serverSocketInstance.getOutputStream());

            while (true) {
                String received = clientInput.readUTF();
                if (received.equalsIgnoreCase("exit")) break;
                updateServerUI("Client: " + received);
            }

        } catch (IOException e) {
            updateServerUI("Error: " + e.getMessage());
        }
    }

    private void updateServerUI(String msg) {
        Platform.runLater(() -> serverChatArea.appendText(msg + "\n"));
    }

    @FXML
    void startClient(ActionEvent event) {
        Stage stage = new Stage();
        Group root = new Group();

        Label title = new Label("User 1");
        title.setLayoutX(100);
        title.setLayoutY(20);

        msgText = new TextField();
        msgText.setPromptText("Enter message");
        msgText.setLayoutX(50);
        msgText.setLayoutY(60);
        msgText.setPrefWidth(300);

        Button connectBtn = new Button("Connect");
        connectBtn.setLayoutX(370);
        connectBtn.setLayoutY(60);
        connectBtn.setOnAction(this::connectToServer);

        Button sendBtn = new Button("Send");
        sendBtn.setLayoutX(200);
        sendBtn.setLayoutY(100);
        sendBtn.setOnAction(e -> sendMessageToServer());

        clientChatArea = new TextArea();
        clientChatArea.setLayoutX(50);
        clientChatArea.setLayoutY(140);
        clientChatArea.setPrefSize(400, 100);
        clientChatArea.setEditable(false);

        root.getChildren().addAll(title, msgText, connectBtn, sendBtn, clientChatArea);
        Scene scene = new Scene(root, 500, 300);
        stage.setScene(scene);
        stage.setTitle("Client");
        stage.show();
    }

    private void connectToServer(ActionEvent event) {
        try {
            clientSocket = new Socket("localhost", 6666);
            serverInput = new DataInputStream(clientSocket.getInputStream());
            serverOutput = new DataOutputStream(clientSocket.getOutputStream());

            updateClientUI("Connected to server.");

            new Thread(() -> {
                try {
                    while (true) {
                        String response = serverInput.readUTF();
                        updateClientUI("Server: " + response);
                    }
                } catch (IOException e) {
                    updateClientUI("Disconnected.");
                }
            }).start();

        } catch (IOException e) {
            updateClientUI("Connection error: " + e.getMessage());
        }
    }

    private void sendMessageToServer() {
        String message = msgText.getText();
        if (message.isEmpty()) return;

        try {
            serverOutput.writeUTF(message);
            updateClientUI("Me: " + message);
            msgText.clear();
        } catch (IOException e) {
            updateClientUI("Failed to send: " + e.getMessage());
        }
    }

    private void updateClientUI(String msg) {
        Platform.runLater(() -> clientChatArea.appendText(msg + "\n"));
    }
}
