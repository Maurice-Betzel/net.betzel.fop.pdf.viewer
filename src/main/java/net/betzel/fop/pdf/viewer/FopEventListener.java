/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.betzel.fop.pdf.viewer;

import org.apache.fop.events.Event;
import org.apache.fop.events.EventFormatter;
import org.apache.fop.events.EventListener;
import org.apache.fop.events.model.EventSeverity;

/**
 *
 * @author mbetzel
 */
public class FopEventListener implements EventListener {

    @Override
    public void processEvent(Event event) {
        String message = EventFormatter.format(event);
        EventSeverity severity = event.getSeverity();
        if (severity == EventSeverity.INFO) {
            System.out.println("[INFO ] " + message);
        } else if (severity == EventSeverity.WARN) {
            System.out.println("[WARN ] " + message);
        } else if (severity == EventSeverity.ERROR) {
            System.err.println("[ERROR] " + message);
        } else if (severity == EventSeverity.FATAL) {
            System.err.println("[FATAL] " + message);
        } else {
            assert false;
        }
    }
    
}