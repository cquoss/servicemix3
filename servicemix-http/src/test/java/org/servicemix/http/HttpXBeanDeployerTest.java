package org.servicemix.http;

import java.io.File;
import java.net.URI;
import java.net.URL;

import javax.jbi.messaging.ExchangeStatus;
import javax.jbi.messaging.InOut;
import javax.xml.namespace.QName;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.servicemix.client.DefaultServiceMixClient;
import org.servicemix.components.util.EchoComponent;
import org.servicemix.jbi.container.ActivationSpec;
import org.servicemix.jbi.container.JBIContainer;
import org.servicemix.jbi.jaxp.SourceTransformer;
import org.servicemix.jbi.jaxp.StringSource;

import junit.framework.TestCase;

public class HttpXBeanDeployerTest extends TestCase {

    private static Log logger =  LogFactory.getLog(HttpSpringTest.class);

    protected JBIContainer container;
    
    protected void setUp() throws Exception {
        container = new JBIContainer();
        container.setUseMBeanServer(false);
        container.setCreateMBeanServer(false);
        container.setEmbedded(true);
        container.init();
    }
    
    protected void tearDown() throws Exception {
        if (container != null) {
            container.shutDown();
        }
    }

    public void test() throws Exception {
        // HTTP Component
        HttpComponent component = new HttpComponent();
        container.activateComponent(component, "HTTPComponent");
        
        // Add a receiver component
        ActivationSpec asEcho = new ActivationSpec("echo", new EchoComponent());
        asEcho.setEndpoint("myConsumer");
        asEcho.setService(new QName("http://test", "MyConsumerService"));
        container.activateComponent(asEcho);
        
        // Start container
        container.start();

        // Deploy SU
        URL url = getClass().getClassLoader().getResource("xbean/xbean.xml");
        File path = new File(new URI(url.toString()));
        path = path.getParentFile();
        component.getServiceUnitManager().deploy("xbean", path.getAbsolutePath());
        component.getServiceUnitManager().start("xbean");
        
        // Test
        DefaultServiceMixClient client = new DefaultServiceMixClient(container);
        InOut me = client.createInOutExchange();
        me.setService(new QName("http://test", "MyProviderService"));
        me.getInMessage().setContent(new StringSource("<echo xmlns='http://test'><echoin0>world</echoin0></echo>"));
        client.sendSync(me);
        if (me.getStatus() == ExchangeStatus.ERROR) {
            if (me.getFault() != null) {
                fail("Received fault: " + new SourceTransformer().toString(me.getFault().getContent()));
            } else if (me.getError() != null) {
                throw me.getError();
            } else {
                fail("Received ERROR status");
            }
        } else {
            logger.info(new SourceTransformer().toString(me.getOutMessage().getContent()));
        }
    }
    
}
