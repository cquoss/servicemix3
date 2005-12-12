/** 
 * <a href="http://servicemix.org">ServiceMix: The open source ESB</a> 
 * 
 * Copyright 2005 RAJD Consultancy Ltd
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
package org.servicemix.jbi.nmr.flow.jms;

import org.activemq.broker.BrokerService;
import org.activemq.xbean.BrokerFactoryBean;
import org.servicemix.jbi.container.JBIContainer;
import org.servicemix.jbi.nmr.flow.Flow;
import org.springframework.core.io.ClassPathResource;

import junit.framework.TestCase;

public class MultipleJMSFlowTest extends TestCase {

    protected BrokerService broker;
    
    protected void setUp() throws Exception {
        BrokerFactoryBean bfb = new BrokerFactoryBean(new ClassPathResource("org/servicemix/jbi/nmr/flow/jca/broker.xml"));
        bfb.afterPropertiesSet();
        broker = bfb.getBroker();
        broker.start();
        super.setUp();
    }
    
    protected void tearDown() throws Exception {
        super.tearDown();
        broker.stop();
    }
    
    public void test() throws Exception {
        JBIContainer[] containers = new JBIContainer[4];
        for (int i = 0; i < containers.length; i++) {
            containers[i] = new JBIContainer();
            containers[i].setName("container" + i);
            containers[i].setFlow(createFlow());
            containers[i].setEmbedded(true);
            containers[i].setMonitorInstallationDirectory(false);
            containers[i].setUseMBeanServer(false);
            containers[i].setCreateMBeanServer(false);
            containers[i].init();
        }
        long t0 = System.currentTimeMillis();
        for (int i = 0; i < containers.length; i++) {
            containers[i].start();
        }
        long t1 = System.currentTimeMillis();
        System.err.println(t1 - t0);
        for (int i = 0; i < containers.length; i++) {
            containers[i].stop();
        }
        for (int i = 0; i < containers.length; i++) {
            containers[i].start();
        }
        for (int i = 0; i < containers.length; i++) {
            containers[i].stop();
        }
        for (int i = 0; i < containers.length; i++) {
            containers[i].shutDown();
        }
    }
    
    protected Flow createFlow() {
        JMSFlow flow = new JMSFlow();
        flow.setJmsURL("tcp://localhost:61216");
        return flow;
    }
    
}
