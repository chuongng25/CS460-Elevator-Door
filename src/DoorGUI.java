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

    private final DoorTwin twin = new DoorTwin(1);

    // Commands go through the five agreed methods only.
    private final DoorApi door = twin;

    // Door leaves travel 200 px each; closed at 600/800, open at 400/1000.
    private static final double LEAF_TRAVEL_PX = 200;
    private static final double LEFT_CLOSED_X = 600;
    private static final double RIGHT_CLOSED_X = 800;

    private Rectangle doorLeft;
    private Rectangle doorRight;
    private Rectangle obstructedOut;
    private Label readout;
    private Button beamButton;
    private Button sillButton;

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

    private void refresh() {
        double p = twin.getPosition();
        doorLeft.setTranslateX(LEFT_CLOSED_X - LEAF_TRAVEL_PX * p);
        doorRight.setTranslateX(RIGHT_CLOSED_X + LEAF_TRAVEL_PX * p);

        obstructedOut.setFill(door.hasObstruction() ? Color.RED : Color.GREEN);

        beamButton.setText(twin.isBeamBroken() ? "Light Curtain: BROKEN" : "Light Curtain: clear");
        sillButton.setText(twin.isSillLoaded() ? "Sill Sensor: LOADED" : "Sill Sensor: clear");

        readout.setText(String.format("%s   %.0f%%", twin.getState(), p * 100));
    }

    private void initialize(StackPane root) {
        Rectangle background = new Rectangle(0, 0, 1200, 800);
        background.setFill(Color.web("#8e9aa3"));
        StackPane.setAlignment(background, Pos.TOP_LEFT);
        root.getChildren().add(background);

        Rectangle sidePanel = new Rectangle(0, 0, 400, 800);
        sidePanel.setFill(Color.SLATEGRAY);
        StackPane.setAlignment(sidePanel, Pos.TOP_LEFT);
        root.getChildren().add(sidePanel);

        Label title = new Label("Elevator Door Assembly");
        title.setTextFill(Color.WHITE);
        StackPane.setAlignment(title, Pos.TOP_LEFT);
        title.setTranslateX(50);
        title.setTranslateY(50);
        root.getChildren().add(title);

        beamButton = new Button("Light Curtain: clear");
        beamButton.setPrefSize(200, 25);
        StackPane.setAlignment(beamButton, Pos.TOP_LEFT);
        beamButton.setTranslateX(50);
        beamButton.setTranslateY(100);
        beamButton.setOnAction(e -> toggleBeam());
        root.getChildren().add(beamButton);

        sillButton = new Button("Sill Sensor: clear");
        sillButton.setPrefSize(200, 25);
        StackPane.setAlignment(sillButton, Pos.TOP_LEFT);
        sillButton.setTranslateX(50);
        sillButton.setTranslateY(150);
        sillButton.setOnAction(e -> toggleSill());
        root.getChildren().add(sillButton);

        Button openDoor = new Button("Open Door");
        openDoor.setPrefSize(200, 25);
        StackPane.setAlignment(openDoor, Pos.TOP_LEFT);
        openDoor.setTranslateX(50);
        openDoor.setTranslateY(250);
        openDoor.setOnAction(e -> handleOpen());
        root.getChildren().add(openDoor);

        Button closeDoor = new Button("Close Door");
        closeDoor.setPrefSize(200, 25);
        StackPane.setAlignment(closeDoor, Pos.TOP_LEFT);
        closeDoor.setTranslateX(50);
        closeDoor.setTranslateY(300);
        closeDoor.setOnAction(e -> handleClose());
        root.getChildren().add(closeDoor);

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

        readout = new Label("");
        readout.setTextFill(Color.WHITE);
        StackPane.setAlignment(readout, Pos.TOP_LEFT);
        readout.setTranslateX(50);
        readout.setTranslateY(400);
        root.getChildren().add(readout);

        // Doorway, behind the leaves.
        Rectangle doorway = new Rectangle(400, 500);
        doorway.setFill(Color.web("#2b2b2b"));
        StackPane.setAlignment(doorway, Pos.TOP_LEFT);
        doorway.setTranslateX(600);
        doorway.setTranslateY(150);
        root.getChildren().add(doorway);

        doorLeft = leaf(LEFT_CLOSED_X);
        root.getChildren().add(doorLeft);

        doorRight = leaf(RIGHT_CLOSED_X);
        root.getChildren().add(doorRight);

        // Wall over the leaves, so they retract out of sight. Order matters.
        root.getChildren().add(wall(400));
        root.getChildren().add(wall(1000));

        // Frame last, over everything.
        Rectangle frame = new Rectangle(400, 500);
        frame.setFill(Color.TRANSPARENT);
        frame.setStroke(Color.web("#3d4a52"));
        frame.setStrokeWidth(6);
        StackPane.setAlignment(frame, Pos.TOP_LEFT);
        frame.setTranslateX(600);
        frame.setTranslateY(150);
        root.getChildren().add(frame);
    }

    private Rectangle wall(double x) {
        Rectangle r = new Rectangle(200, 500);
        r.setFill(Color.web("#8e9aa3"));
        StackPane.setAlignment(r, Pos.TOP_LEFT);
        r.setTranslateX(x);
        r.setTranslateY(150);
        return r;
    }

    private Rectangle leaf(double closedX) {
        Rectangle r = new Rectangle(200, 500);
        r.setFill(Color.web("#b4bcc2"));
        r.setStroke(Color.web("#6e7a82"));
        r.setStrokeWidth(2);
        StackPane.setAlignment(r, Pos.TOP_LEFT);
        r.setTranslateX(closedX);
        r.setTranslateY(150);
        return r;
    }

    @Override
    public void start(Stage primaryStage) {
        StackPane root = new StackPane();
        initialize(root);

        // Poll real position each frame. A fixed-length animation would draw
        // the wrong thing during a safety reversal.
        new AnimationTimer() {
            public void handle(long now) {
                refresh();
            }
        }.start();

        primaryStage.setTitle("Elevator Door");
        primaryStage.setScene(new Scene(root, 1200, 800));
        primaryStage.setOnHidden(e -> twin.stop());
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
