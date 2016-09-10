/*
 * Copyright 2016 betzel.net.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
public class ProgressDialog {

    private final Stage dialogStage = new Stage();
    private final ProgressIndicator progressIndicator = new ProgressIndicator();

    public ProgressDialog() {
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