/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.betzel.fop.pdf.viewer;

import javafx.beans.property.DoubleProperty;
import javafx.beans.value.ObservableValue;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.Background;
import javafx.scene.layout.HBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

/**
 *
 * @author mbetzel
 */
public class ScanProgressDialog {

    private final Stage dialogStage = new Stage();
    private final ProgressIndicator progressIndicator = new ProgressIndicator();

    public ScanProgressDialog() {
        dialogStage.initStyle(StageStyle.TRANSPARENT);
        dialogStage.setResizable(false);
        dialogStage.initModality(Modality.APPLICATION_MODAL);
        final HBox hBox = new HBox();
        hBox.setAlignment(Pos.CENTER);
        hBox.setBackground(Background.EMPTY);
        hBox.getChildren().addAll(progressIndicator);
        Scene scene = new Scene(hBox);
        scene.setFill(null);
        dialogStage.setScene(scene);
    }
    
    public DoubleProperty getProgressProperty() {
        return progressIndicator.progressProperty();
    }

    public void show() {
        progressIndicator.progressProperty().setValue(-1);
        dialogStage.show();
    }
    
    public void show(ObservableValue<? extends Number> observable) {
        progressIndicator.progressProperty().bind(observable);
        dialogStage.show();
    }

    public void close() {
        progressIndicator.progressProperty().unbind();
        dialogStage.close();
    }

}
