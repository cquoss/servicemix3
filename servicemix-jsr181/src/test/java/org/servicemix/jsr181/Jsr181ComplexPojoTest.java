/** 
 * 
 * Copyright 2005 LogicBlaze, Inc. http://www.logicblaze.com
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); 
 * you may not use this file except in compliance with the License. 
 * You may obtain a copy of the License at 
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, 
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. 
 * See the License for the specific language governing permissions and 
 * limitations under the License. 
 * 
 **/
package org.servicemix.jsr181;

import javax.jbi.component.ComponentContext;
import javax.jbi.messaging.DeliveryChannel;
import javax.jbi.messaging.ExchangeStatus;
import javax.jbi.messaging.InOnly;
import javax.jbi.messaging.InOut;
import javax.jbi.messaging.MessageExchangeFactory;
import javax.jbi.messaging.NormalizedMessage;
import javax.naming.InitialContext;
import javax.xml.namespace.QName;
import javax.xml.transform.Source;

import junit.framework.TestCase;

import org.servicemix.client.DefaultServiceMixClient;
import org.servicemix.components.util.EchoComponent;
import org.servicemix.jbi.container.ActivationSpec;
import org.servicemix.jbi.container.JBIContainer;
import org.servicemix.jbi.jaxp.StringSource;
import org.servicemix.tck.ReceiverComponent;

public class Jsr181ComplexPojoTest extends TestCase {

    //private static Log logger =  LogFactory.getLog(Jsr181ComponentTest.class);
    
    protected JBIContainer container;
    
    protected void setUp() throws Exception {
        container = new JBIContainer();
        container.setUseMBeanServer(false);
        container.setCreateMBeanServer(false);
        container.setMonitorInstallationDirectory(false);
        container.setNamingContext(new InitialContext());
        container.setEmbedded(true);
        container.setFlowName("st");
        container.init();
    }
    
    protected void tearDown() throws Exception {
        if (container != null) {
            container.shutDown();
        }
    }
    
    public void testComplexOneWay() throws Exception {
        Jsr181SpringComponent component = new Jsr181SpringComponent();
        Jsr181Endpoint endpoint = new Jsr181Endpoint();
        endpoint.setPojo(new ComplexPojoImpl());
        endpoint.setServiceInterface(ComplexPojo.class.getName());
        component.setEndpoints(new Jsr181Endpoint[] { endpoint });
        container.activateComponent(component, "JSR181Component");
        
        ReceiverComponent receiver = new ReceiverComponent();
        container.activateComponent(receiver, "Receiver");
        
        container.start();

        DefaultServiceMixClient client = new DefaultServiceMixClient(container);
        InOut me = client.createInOutExchange();
        me.setInterfaceName(new QName("http://jsr181.servicemix.org", "ComplexPojoPortType"));
        me.getInMessage().setContent(new StringSource("<oneWay xmlns='http://jsr181.servicemix.org'><in0>world</in0></oneWay>"));
        client.sendSync(me);
        
        // Wait all acks being processed
        Thread.sleep(100);
    }
    
    public void testComplexTwoWay() throws Exception {
        Jsr181SpringComponent component = new Jsr181SpringComponent();
        Jsr181Endpoint endpoint = new Jsr181Endpoint();
        endpoint.setPojo(new ComplexPojoImpl());
        endpoint.setServiceInterface(ComplexPojo.class.getName());
        component.setEndpoints(new Jsr181Endpoint[] { endpoint });
        container.activateComponent(component, "JSR181Component");
        
        EchoComponent echo = new EchoComponent();
        ActivationSpec as = new ActivationSpec();
        as.setComponent(echo);
        as.setService(ReceiverComponent.SERVICE);
        as.setComponentName("Echo");
        container.activateComponent(as);
        
        container.start();

        DefaultServiceMixClient client = new DefaultServiceMixClient(container);
        InOut me = client.createInOutExchange();
        me.setInterfaceName(new QName("http://jsr181.servicemix.org", "ComplexPojoPortType"));
        me.getInMessage().setContent(new StringSource("<twoWay xmlns='http://jsr181.servicemix.org'><in0>world</in0></twoWay>"));
        client.sendSync(me);
        client.done(me);
        
        // Wait all acks being processed
        Thread.sleep(100);
    }
    
    public interface ComplexPojo {
        public void oneWay(Source src) throws Exception;
        public Source twoWay(Source src) throws Exception;
    }
    
    public static class ComplexPojoImpl implements ComplexPojo {
        private ComponentContext context;

        public ComponentContext getContext() {
            return context;
        }

        public void setContext(ComponentContext context) {
            this.context = context;
        }
        
        public void oneWay(Source src) throws Exception {
            DeliveryChannel channel = context.getDeliveryChannel();
            MessageExchangeFactory factory = channel.createExchangeFactory();
            InOnly inonly = factory.createInOnlyExchange();
            inonly.setService(ReceiverComponent.SERVICE);
            NormalizedMessage msg = inonly.createMessage();
            msg.setContent(src);
            inonly.setInMessage(msg);
            channel.send(inonly);
        }
        
        public Source twoWay(Source src) throws Exception {
            DeliveryChannel channel = context.getDeliveryChannel();
            MessageExchangeFactory factory = channel.createExchangeFactory();
            InOut inout = factory.createInOutExchange();
            inout.setService(ReceiverComponent.SERVICE);
            NormalizedMessage msg = inout.createMessage();
            msg.setContent(src);
            inout.setInMessage(msg);
            channel.sendSync(inout);
            Source outSrc = inout.getOutMessage().getContent();
            inout.setStatus(ExchangeStatus.DONE);
            channel.send(inout);
            return outSrc;
        }
    }
    
}
