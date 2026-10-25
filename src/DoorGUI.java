import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.stage.Stage;

/**
 * JavaFX View: Graphical visualization of the elevator door and control panel.
 */
public class DoorGUI extends Application {

    @Override
    public void start(Stage primaryStage) {
        StackPane root = new StackPane(new Label("Elevator Door Simulator (Skeleton)"));
        Scene scene = new Scene(root, 400, 300);
        primaryStage.setTitle("CS460 Team #01 - Elevator Door Simulator");
        primaryStage.setScene(scene);
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
