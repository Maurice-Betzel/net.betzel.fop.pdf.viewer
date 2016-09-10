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

import javafx.application.Platform;
import javafx.scene.control.TextArea;
import org.apache.fop.events.Event;
import org.apache.fop.events.EventFormatter;
import org.apache.fop.events.EventListener;
import org.apache.fop.events.model.EventSeverity;

/**
 *
 * @author mbetzel
 */
public class FopEventListener implements EventListener {

    private final TextArea textArea;
    
    public FopEventListener(TextArea textArea) {
        this.textArea = textArea;
    }

    @Override
    public void processEvent(Event event) {
        String message = EventFormatter.format(event);
        EventSeverity severity = event.getSeverity();
        if (severity == EventSeverity.INFO) {
            Platform.runLater(() -> {
                textArea.appendText("[FOP_INFO] " + message + "\n");
            });
        } else if (severity == EventSeverity.WARN) {
            Platform.runLater(() -> {
                textArea.appendText("[FOP_WARN] " + message + "\n");
            });
        } else if (severity == EventSeverity.ERROR) {
            Platform.runLater(() -> {
                textArea.appendText("[FOP_ERROR] " + message + "\n");
            });
        } else if (severity == EventSeverity.FATAL) {
            Platform.runLater(() -> {
                textArea.appendText("[FOP_FATAL] " + message + "\n");
            });
        } else {
            assert false;
        }
    }
    
}