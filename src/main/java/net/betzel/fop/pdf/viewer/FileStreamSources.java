/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package net.betzel.fop.pdf.viewer;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import javax.xml.transform.Source;
import javax.xml.transform.stream.StreamSource;

/**
 *
 * @author mbetzel
 */
public class FileStreamSources {
    
    private final Source xmlSource;
    private final Source xslSource;

    public FileStreamSources(String xml, String xsl) {
        xmlSource = new StreamSource(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
        xslSource = new StreamSource(new ByteArrayInputStream(xsl.getBytes(StandardCharsets.UTF_8)));
    }

    public Source getXmlSource() {
        return xmlSource;
    }

    public Source getXslSource() {
        return xslSource;
    }
    
}
