import javafx.application.Application;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.*;
import javafx.stage.Stage;
import javafx.animation.TranslateTransition;
import javafx.util.Duration;


/**
 * JavaFX View: Graphical visualization of the elevator door and control panel.
 */
public class DoorGUI extends Application {
    private DoorManager door = new DoorManager();

    private TextField hostInput;
    private TextField socketInput;

    private Button connectButton;
    private Button disconnectButton;
    private Button openDoor;
    private Button closeDoor;

    private Rectangle connectStatus;
    private Rectangle obstructedOut;
    private Rectangle doorStatusOut;

    private Rectangle doorRight;
    private Rectangle doorLeft;

    private void animateClose() {
        TranslateTransition leftAnimation =
                new TranslateTransition(Duration.seconds(2), doorLeft);

        leftAnimation.setToX(0);

        TranslateTransition rightAnimation =
                new TranslateTransition(Duration.seconds(2), doorRight);

        rightAnimation.setToX(0);

        leftAnimation.play();
        rightAnimation.play();
    }

    private void animateOpen() {
        TranslateTransition leftAnimation =
                new TranslateTransition(Duration.seconds(2), doorLeft);

        leftAnimation.setToX(-200);

        TranslateTransition rightAnimation =
                new TranslateTransition(Duration.seconds(2), doorRight);

        rightAnimation.setToX(200);

        leftAnimation.play();
        rightAnimation.play();
    }

    private void updateDoorStatus() throws Exception {
        boolean open = door.isFullyOpen();
        boolean closed = door.isFullyClosed();
        boolean obstruction = door.hasObstruction();

        // Green means open; red means closed; yellow means moving.
        if (open) {
            doorStatusOut.setFill(Color.GREEN);
        } else if (closed) {
            doorStatusOut.setFill(Color.RED);
        } else {
            doorStatusOut.setFill(Color.YELLOW);
        }

        obstructedOut.setFill(
                obstruction ? Color.RED : Color.GREEN
        );
    }

    private void handleConnect(){
        try {
            String host = hostInput.getText().trim();
            String portText = socketInput.getText().trim();

            if (host.isEmpty() || portText.isEmpty()) {
                System.out.println("Enter a host and port.");
                return;
            }

            int port = Integer.parseInt(portText);

            if (port < 1 || port > 65535) {
                System.out.println("Port must be between 1 and 65535.");
                return;
            }

            door.connect(host, port);

            connectStatus.setFill(Color.GREEN);
            System.out.println("Connected to " + host + ":" + port);

        } catch (NumberFormatException ex) {
            System.out.println("Invalid port number.");
        } catch (Exception ex) {
            connectStatus.setFill(Color.RED);
            System.out.println("Connection failed: " + ex.getMessage());
        }
    }
    private void handleDisconnect(){
        door.disconnect();
        connectStatus.setFill(Color.RED);
        System.out.println("Disconnected.");
    }
    private void handleOpen(){
        try {
            if (!door.isConnected()) {
                System.out.println("Connect to the server first.");
                return;
            }

            door.open();
            animateOpen();
            updateDoorStatus();

        } catch (Exception ex) {
            System.out.println("Open failed: " + ex.getMessage());
        }
    }
    private void handleClose(){
        try {
            if (!door.isConnected()) {
                System.out.println("Connect to the server first.");
                return;
            }

            if (door.hasObstruction()) {
                obstructedOut.setFill(Color.RED);
                System.out.println("Cannot close: obstruction detected.");
                return;
            }
            animateClose();
            door.close();
            updateDoorStatus();

        } catch (Exception ex) {
            System.out.println("Close failed: " + ex.getMessage());
        }
    }

    private void initialize(Scene scene, StackPane root){
        Rectangle background = new Rectangle(0,0,1200,800);
        background.setFill(Color.LIGHTGRAY);
        StackPane.setAlignment(background, Pos.TOP_LEFT);
        root.getChildren().add(background);

        Rectangle sidePanel = new Rectangle(0,0,400,800);
        sidePanel.setFill(Color.SLATEGRAY);
        StackPane.setAlignment(sidePanel, Pos.TOP_LEFT);
        root.getChildren().add(sidePanel);

        hostInput = new TextField();
        hostInput.setMaxWidth(300);
        StackPane.setAlignment(hostInput,Pos.TOP_LEFT);
        hostInput.setTranslateX(50);
        hostInput.setTranslateY(50);
        root.getChildren().add(hostInput);

        socketInput = new TextField();
        socketInput.setMaxWidth(300);
        StackPane.setAlignment(socketInput,Pos.TOP_LEFT);
        socketInput.setTranslateX(50);
        socketInput.setTranslateY(100);
        root.getChildren().add(socketInput);

        connectButton = new Button("Connect");
        connectButton.setPrefSize(100 ,25);
        StackPane.setAlignment(connectButton,Pos.TOP_LEFT);
        connectButton.setTranslateX(50);
        connectButton.setTranslateY(150);
        root.getChildren().add(connectButton);
        connectButton.setOnAction(e -> handleConnect());

        disconnectButton = new Button("Disonnect");
        disconnectButton.setPrefSize(100 ,25);
        StackPane.setAlignment(disconnectButton,Pos.TOP_LEFT);
        disconnectButton.setTranslateX(175);
        disconnectButton.setTranslateY(150);
        root.getChildren().add(disconnectButton);
        disconnectButton.setOnAction(e -> handleDisconnect());

        connectStatus = new Rectangle(0,0,25,25);
        connectStatus.setFill(Color.RED);
        StackPane.setAlignment(connectStatus, Pos.TOP_LEFT);
        connectStatus.setTranslateX(325);
        connectStatus.setTranslateY(150);
        root.getChildren().add(connectStatus);

        openDoor = new Button("Open Door");
        openDoor.setPrefSize(200 ,25);
        StackPane.setAlignment(openDoor,Pos.TOP_LEFT);
        openDoor.setTranslateX(50);
        openDoor.setTranslateY(250);
        root.getChildren().add(openDoor);
        openDoor.setOnAction(e -> handleOpen());

        closeDoor = new Button("Close Door");
        closeDoor.setPrefSize(200 ,25);
        StackPane.setAlignment(closeDoor,Pos.TOP_LEFT);
        closeDoor.setTranslateX(50);
        closeDoor.setTranslateY(300);
        root.getChildren().add(closeDoor);
        closeDoor.setOnAction(e -> handleClose());

        Rectangle obstructedRect = new Rectangle(150,25);
        obstructedRect.setFill(Color.WHITE);
        StackPane.setAlignment(obstructedRect, Pos.TOP_LEFT);
        obstructedRect.setTranslateX(50);
        obstructedRect.setTranslateY(350);
        root.getChildren().add(obstructedRect);

        Label obstructed = new Label("Obstruction Sensor");
        StackPane.setAlignment(obstructed,Pos.TOP_LEFT);
        obstructed.setTranslateX(75);
        obstructed.setTranslateY(355);
        root.getChildren().add(obstructed);

        obstructedOut = new Rectangle(75, 25);
        obstructedOut.setFill(Color.GREEN);
        StackPane.setAlignment(obstructedOut, Pos.TOP_LEFT);
        obstructedOut.setTranslateX(250);
        obstructedOut.setTranslateY(350);
        root.getChildren().add(obstructedOut);

        Rectangle doorStatusRect = new Rectangle(150,25);
        doorStatusRect.setFill(Color.WHITE);
        StackPane.setAlignment(doorStatusRect, Pos.TOP_LEFT);
        doorStatusRect.setTranslateX(50);
        doorStatusRect.setTranslateY(400);
        root.getChildren().add(doorStatusRect);

        Label doorStatus = new Label("Door Status");
        StackPane.setAlignment(doorStatus,Pos.TOP_LEFT);
        doorStatus.setTranslateX(85);
        doorStatus.setTranslateY(405);
        root.getChildren().add(doorStatus);

        doorStatusOut = new Rectangle(75, 25);
        doorStatusOut.setFill(Color.GREEN);
        StackPane.setAlignment(doorStatusOut, Pos.TOP_LEFT);
        doorStatusOut.setTranslateX(250);
        doorStatusOut.setTranslateY(400);
        root.getChildren().add(doorStatusOut);

        doorLeft = new Rectangle(200,500);
        doorLeft.setFill(Color.SILVER);
        StackPane.setAlignment(doorLeft, Pos.TOP_LEFT);
        doorLeft.setTranslateX(600);
        doorLeft.setTranslateY(150);
        root.getChildren().add(doorLeft);

        doorRight = new Rectangle(200,500);
        doorRight.setFill(Color.SILVER);
        StackPane.setAlignment(doorRight, Pos.TOP_LEFT);
        doorRight.setTranslateX(800);
        doorRight.setTranslateY(150);
        root.getChildren().add(doorRight);
    }
    @Override
    public void start(Stage primaryStage) {
        StackPane root = new StackPane(new Label("Elevator Door Simulator (Skeleton)"));
        Scene scene = new Scene(root, 1200, 800);
        initialize(scene,root);
        primaryStage.setTitle("CS460 Team #01 - Elevator Door Simulator");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
