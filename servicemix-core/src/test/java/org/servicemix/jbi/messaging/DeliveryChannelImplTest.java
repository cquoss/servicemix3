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
package org.servicemix.jbi.messaging;

import javax.jbi.messaging.DeliveryChannel;
import javax.jbi.messaging.MessageExchangeFactory;
import javax.jbi.messaging.MessagingException;

import junit.framework.TestCase;

import org.servicemix.jbi.container.ActivationSpec;
import org.servicemix.jbi.container.JBIContainer;
import org.servicemix.tck.SenderComponent;

public class DeliveryChannelImplTest extends TestCase {

    protected JBIContainer container;
    protected DeliveryChannel channel;
    
    protected void setUp() throws Exception {
        container = new JBIContainer();
        container.init();
        container.start();
        SenderComponent component = new SenderComponent();
        container.activateComponent(new ActivationSpec("sender", component));
        channel = component.getDeliveryChannel();
    }
    
    protected void tearDown() throws Exception {
        container.shutDown();
    }
    
    public void testExchangeFactoryOnOpenChannel() throws Exception {
        MessageExchangeFactory mef = channel.createExchangeFactory();
        assertNotNull(mef);
        assertNotNull(mef.createInOnlyExchange());
    }
    
    public void testExchangeFactoryOnClosedChannel() throws Exception {
        channel.close();
        MessageExchangeFactory mef = channel.createExchangeFactory();
        assertNotNull(mef);
        try {
            mef.createInOnlyExchange();
            fail("Exchange creation should have failed (JBI: 5.5.2.1.4)");
        } catch (MessagingException e) {
            // expected
        }
    }
    
}
