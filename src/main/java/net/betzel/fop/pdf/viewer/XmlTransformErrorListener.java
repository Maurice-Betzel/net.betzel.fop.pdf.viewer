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
import javax.xml.transform.ErrorListener;
import javax.xml.transform.TransformerException;

/**
 *
 * @author mbetzel
 */
public class XmlTransformErrorListener implements ErrorListener {

    private final TextArea textArea;

    public XmlTransformErrorListener(TextArea textArea) {
        this.textArea = textArea;
    }

    @Override
    public void warning(TransformerException e) throws TransformerException {
        Platform.runLater(() -> {
            textArea.appendText("[XML_WARN] " + e.getMessage() + "\n");
        });
        throw (e);
    }

    @Override
    public void error(TransformerException e) throws TransformerException {
        Platform.runLater(() -> {
            textArea.appendText("[XML_ERROR] " + e.getMessage() + "\n");
        });
        throw (e);
    }

    @Override
    public void fatalError(TransformerException e) throws TransformerException {
        Platform.runLater(() -> {
            textArea.appendText("[XML_FATAL] " + e.getMessage() + "\n");
        });
        throw (e);
    }
    
}