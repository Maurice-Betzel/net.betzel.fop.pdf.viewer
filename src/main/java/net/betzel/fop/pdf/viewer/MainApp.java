package net.betzel.fop.pdf.viewer;

import javafx.application.Application;
import static javafx.application.Application.launch;
import javafx.application.Platform;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.layout.AnchorPane;
import javafx.stage.Stage;
import static javafx.application.Application.launch;


public class MainApp extends Application {
    
    private FXMLController fxmlController;
    private static Stage primaryStage;

    @Override
    public void start(Stage stage) throws Exception {
        primaryStage = stage;
        FXMLLoader loader = new FXMLLoader();
        AnchorPane root = loader.load(getClass().getResource("/fxml/Scene.fxml").openStream());
        fxmlController = loader.getController();
        //Parent root = FXMLLoader.load(getClass().getResource("/fxml/Scene.fxml"));
        
        Scene scene = new Scene(root);
        scene.getStylesheets().add("/styles/Styles.css");
        
        stage.setTitle("XSLT 2.0 FOP PDF Viewer");
        stage.setScene(scene);
        stage.show();
        System.setProperty("sun.java2d.cmm", "sun.java2d.cmm.kcms.KcmsServiceProvider");        
    }
    
    @Override
    public void stop() throws Exception {
        super.stop();
        fxmlController.shutDown();
        Platform.exit();
        System.exit(0);
    }
    
    protected static Stage getPrimaryStage() {
        return primaryStage;
    }

    /**
     * The main() method is ignored in correctly deployed JavaFX application.
     * main() serves only as fallback in case the application can not be
     * launched through deployment artifacts, e.g., in IDEs with limited FX
     * support. NetBeans ignores main().
     *
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        launch(args);
    }

}
