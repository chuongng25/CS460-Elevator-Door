import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.Stage;

public class DoorGUI extends Application {
    //I dont really know how we are going to show 4 different doors.
    private final DoorTwin twin = new DoorTwin(1);

    //Only API methods
    private final DoorApi door = twin;

    //Door leaves travel 200 px each; closed at 600/800, open at 400/1000.
    private static final double LEAF_TRAVEL_PX = 200;
    private static final double LEFT_CLOSED_X = 600;
    private static final double RIGHT_CLOSED_X = 800;

    private Button beamButton;
    private Button sillButton;
    private Button openDoor;
    private Button closeDoor;

    private Rectangle obstructedOut;
    private Rectangle doorStatusOut;

    private Rectangle doorRight;
    private Rectangle doorLeft;

    private void updateDoorStatus() {
        double p = twin.getPosition();
        doorLeft.setTranslateX(LEFT_CLOSED_X - LEAF_TRAVEL_PX * p);
        doorRight.setTranslateX(RIGHT_CLOSED_X + LEAF_TRAVEL_PX * p);

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

        beamButton.setText(twin.isBeamBroken() ? "Light Beam: BROKEN" : "Light Beam: clear");
        sillButton.setText(twin.isSillLoaded() ? "Sill Sensor: LOADED" : "Sill Sensor: clear");
    }

    private void handleOpen() {
        door.open();
    }

    private void handleClose() {
        door.close();
    }

    private void toggleBeam() {
        twin.setBeamBroken(!twin.isBeamBroken());
    }

    private void toggleSill() {
        twin.setSillLoaded(!twin.isSillLoaded());
    }

    private void initialize(Scene scene, StackPane root) {
        Rectangle background = new Rectangle(0, 0, 1200, 800);
        background.setFill(Color.TAN);
        StackPane.setAlignment(background, Pos.TOP_LEFT);
        root.getChildren().add(background);

        Rectangle doorway = new Rectangle(400, 500);
        doorway.setFill(Color.BLACK);
        StackPane.setAlignment(doorway, Pos.TOP_LEFT);
        doorway.setTranslateX(600);
        doorway.setTranslateY(150);
        root.getChildren().add(doorway);

        Rectangle sidePanel = new Rectangle(0, 0, 400, 800);
        sidePanel.setFill(Color.SLATEGRAY);
        StackPane.setAlignment(sidePanel, Pos.TOP_LEFT);
        root.getChildren().add(sidePanel);

        beamButton = new Button("Light Beam: clear");
        beamButton.setPrefSize(200, 25);
        StackPane.setAlignment(beamButton, Pos.TOP_LEFT);
        beamButton.setTranslateX(50);
        beamButton.setTranslateY(100);
        root.getChildren().add(beamButton);
        beamButton.setOnAction(e -> toggleBeam());

        sillButton = new Button("Sill Sensor: clear");
        sillButton.setPrefSize(200, 25);
        StackPane.setAlignment(sillButton, Pos.TOP_LEFT);
        sillButton.setTranslateX(50);
        sillButton.setTranslateY(150);
        root.getChildren().add(sillButton);
        sillButton.setOnAction(e -> toggleSill());

        openDoor = new Button("Open Door");
        openDoor.setPrefSize(200, 25);
        StackPane.setAlignment(openDoor, Pos.TOP_LEFT);
        openDoor.setTranslateX(50);
        openDoor.setTranslateY(250);
        root.getChildren().add(openDoor);
        openDoor.setOnAction(e -> handleOpen());

        closeDoor = new Button("Close Door");
        closeDoor.setPrefSize(200, 25);
        StackPane.setAlignment(closeDoor, Pos.TOP_LEFT);
        closeDoor.setTranslateX(50);
        closeDoor.setTranslateY(300);
        root.getChildren().add(closeDoor);
        closeDoor.setOnAction(e -> handleClose());

        Rectangle obstructedRect = new Rectangle(150, 25);
        obstructedRect.setFill(Color.WHITE);
        StackPane.setAlignment(obstructedRect, Pos.TOP_LEFT);
        obstructedRect.setTranslateX(50);
        obstructedRect.setTranslateY(350);
        root.getChildren().add(obstructedRect);

        Label obstructed = new Label("Obstruction Sensor");
        StackPane.setAlignment(obstructed, Pos.TOP_LEFT);
        obstructed.setTranslateX(75);
        obstructed.setTranslateY(355);
        root.getChildren().add(obstructed);

        obstructedOut = new Rectangle(75, 25);
        obstructedOut.setFill(Color.GREEN);
        StackPane.setAlignment(obstructedOut, Pos.TOP_LEFT);
        obstructedOut.setTranslateX(250);
        obstructedOut.setTranslateY(350);
        root.getChildren().add(obstructedOut);

        Rectangle doorStatusRect = new Rectangle(150, 25);
        doorStatusRect.setFill(Color.WHITE);
        StackPane.setAlignment(doorStatusRect, Pos.TOP_LEFT);
        doorStatusRect.setTranslateX(50);
        doorStatusRect.setTranslateY(400);
        root.getChildren().add(doorStatusRect);

        Label doorStatus = new Label("Door Status");
        StackPane.setAlignment(doorStatus, Pos.TOP_LEFT);
        doorStatus.setTranslateX(85);
        doorStatus.setTranslateY(405);
        root.getChildren().add(doorStatus);

        doorStatusOut = new Rectangle(75, 25);
        doorStatusOut.setFill(Color.RED);
        StackPane.setAlignment(doorStatusOut, Pos.TOP_LEFT);
        doorStatusOut.setTranslateX(250);
        doorStatusOut.setTranslateY(400);
        root.getChildren().add(doorStatusOut);

        doorLeft = new Rectangle(200, 500);
        doorLeft.setFill(Color.SILVER);
        //dark outline so you can see the line where the two doors meet
        doorLeft.setStroke(Color.DIMGRAY);
        doorLeft.setStrokeWidth(2);
        StackPane.setAlignment(doorLeft, Pos.TOP_LEFT);
        doorLeft.setTranslateX(600);
        doorLeft.setTranslateY(150);
        root.getChildren().add(doorLeft);

        doorRight = new Rectangle(200, 500);
        doorRight.setFill(Color.SILVER);
        doorRight.setStroke(Color.DIMGRAY);
        doorRight.setStrokeWidth(2);
        StackPane.setAlignment(doorRight, Pos.TOP_LEFT);
        doorRight.setTranslateX(800);
        doorRight.setTranslateY(150);
        root.getChildren().add(doorRight);
    }

    @Override
    public void start(Stage primaryStage) {
        StackPane root = new StackPane();
        Scene scene = new Scene(root, 1200, 800);
        initialize(scene, root);
        //redraw from the twin's real position every frame
        new AnimationTimer() {
            public void handle(long now) {
                updateDoorStatus();
            }
        }.start();
        primaryStage.setTitle("Elevator Door");
        primaryStage.setScene(scene);
        primaryStage.setOnHidden(e -> twin.stop());
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
