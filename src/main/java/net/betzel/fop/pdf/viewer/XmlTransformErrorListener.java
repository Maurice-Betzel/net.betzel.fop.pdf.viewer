package net.betzel.fop.pdf.viewer;

import javax.xml.transform.ErrorListener;
import javax.xml.transform.TransformerException;

/**
 *
 * @author mbetzel
 */
public class XmlTransformErrorListener implements ErrorListener {

    @Override
    public void warning(TransformerException e) throws TransformerException {
        System.err.println("[WARN ] " +e.getMessage());
        throw(e);
    }

    @Override
    public void error(TransformerException e) throws TransformerException {
        System.err.println("[ERROR] " +e.getMessage());
        throw(e);
    }

    @Override
    public void fatalError(TransformerException e) throws TransformerException {
        System.err.println("[FATAL] " + e.getMessage());
        throw(e);
    }
    
}