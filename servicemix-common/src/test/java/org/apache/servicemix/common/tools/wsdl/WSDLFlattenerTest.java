package org.apache.servicemix.common.tools.wsdl;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.net.URL;

import javax.wsdl.Definition;
import javax.wsdl.PortType;
import javax.wsdl.factory.WSDLFactory;
import javax.wsdl.xml.WSDLReader;
import javax.wsdl.xml.WSDLWriter;
import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;

import junit.framework.TestCase;

public class WSDLFlattenerTest extends TestCase {

    public void test() throws Exception {
        URL resource = getClass().getClassLoader().getResource("wsn/wsn.wsdl");
        WSDLReader reader = WSDLFactory.newInstance().newWSDLReader();
        Definition definition = reader.readWSDL(null, resource.toString());
        WSDLFlattener flattener = new WSDLFlattener(definition);
        
        Definition flat = flattener.getDefinition(new QName("http://docs.oasis-open.org/wsn/brw-2", "NotificationBroker"));
        assertNotNull(flat);
        
        // Check that the definition is really standalone
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        WSDLWriter writer = WSDLFactory.newInstance().newWSDLWriter();
        writer.writeWSDL(flat, baos);
        
        System.err.println(baos.toString());
        
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        Document description = factory.newDocumentBuilder().parse(new ByteArrayInputStream(baos.toByteArray()));
        Definition newFlat = WSDLFactory.newInstance().newWSDLReader().readWSDL(null, description);
        assertNotNull(newFlat);
        assertEquals(1, newFlat.getPortTypes().size());
        PortType portType = (PortType) newFlat.getPortTypes().values().iterator().next();
        assertNotNull(portType);
    }
    
}
