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
