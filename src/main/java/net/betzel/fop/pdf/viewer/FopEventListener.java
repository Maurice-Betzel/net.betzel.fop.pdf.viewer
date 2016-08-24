/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.betzel.fop.pdf.viewer;

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
            System.out.println("[FOP_INFO] " + message);
            textArea.appendText("[FOP_INFO] " + message + "\n");
        } else if (severity == EventSeverity.WARN) {
            System.out.println("[FOP_WARN] " + message);
            textArea.appendText("[FOP_WARN] " + message + "\n");
        } else if (severity == EventSeverity.ERROR) {
            System.err.println("[FOP_ERROR] " + message);
            textArea.appendText("[FOP_ERROR] " + message + "\n");
        } else if (severity == EventSeverity.FATAL) {
            System.err.println("[FOP_FATAL] " + message);
            textArea.appendText("[FOP_FATAL] " + message + "\n");
        } else {
            assert false;
        }
    }
    
}