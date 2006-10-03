/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.servicemix.jbi.nmr.flow.jca;

import javax.jbi.messaging.ExchangeStatus;
import javax.jbi.messaging.InOut;
import javax.jbi.messaging.MessageExchange;
import javax.jbi.messaging.MessagingException;
import javax.jbi.messaging.NormalizedMessage;
import javax.naming.Context;
import javax.transaction.TransactionManager;
import javax.xml.namespace.QName;

import junit.framework.TestCase;

import org.apache.activemq.broker.BrokerService;
import org.apache.activemq.store.memory.MemoryPersistenceAdapter;
import org.apache.geronimo.transaction.ExtendedTransactionManager;
import org.apache.geronimo.transaction.context.TransactionContextManager;
import org.apache.geronimo.transaction.manager.TransactionLog;
import org.apache.geronimo.transaction.manager.XidFactoryImpl;
import org.apache.servicemix.MessageExchangeListener;
import org.apache.servicemix.client.DefaultServiceMixClient;
import org.apache.servicemix.components.util.ComponentSupport;
import org.apache.servicemix.jbi.container.ActivationSpec;
import org.apache.servicemix.jbi.container.JBIContainer;
import org.apache.servicemix.jbi.jaxp.StringSource;
import org.apache.servicemix.jbi.nmr.flow.Flow;
import org.apache.xbean.spring.jndi.SpringInitialContextFactory;
import org.jencks.factory.GeronimoTransactionManagerFactoryBean;
import org.jencks.factory.HowlLogFactoryBean;
import org.jencks.factory.TransactionContextManagerFactoryBean;
import org.jencks.factory.TransactionManagerFactoryBean;

/**
 * @version $Revision: 426415 $
 */
public class JcaFlowWithTxLogTest extends TestCase {
    
	private JBIContainer senderContainer = new JBIContainer();
    private JBIContainer receiverContainer = new JBIContainer();
    private BrokerService broker;
	private TransactionManager tm;
    
	public class MyEchoComponent extends ComponentSupport implements MessageExchangeListener {

		public void onMessageExchange(MessageExchange exchange) throws MessagingException {
			if(exchange.getStatus() == ExchangeStatus.ACTIVE && exchange instanceof InOut) {
				InOut inOut = (InOut) exchange;
				NormalizedMessage inMessage = inOut.getInMessage();
				NormalizedMessage outMessage = inOut.createMessage();
				outMessage.setContent(inMessage.getContent());
				inOut.setOutMessage(outMessage);
				send(inOut);
			}
		}

	}

    /*
     * @see TestCase#setUp()
     */
    protected void setUp() throws Exception {
        super.setUp();
        
        System.setProperty(Context.INITIAL_CONTEXT_FACTORY, SpringInitialContextFactory.class.getName());
        System.setProperty(Context.PROVIDER_URL, "jndi.xml");

        XidFactoryImpl xidFactory = new XidFactoryImpl();
        HowlLogFactoryBean howlLogFactoryBean = new HowlLogFactoryBean();
        howlLogFactoryBean.setXidFactory(xidFactory);
        howlLogFactoryBean.setLogFileDir("target/txlog");
        TransactionManagerFactoryBean tmcf = new TransactionManagerFactoryBean();
        tmcf.setTransactionLog((TransactionLog) howlLogFactoryBean.getObject());
        tmcf.afterPropertiesSet();
        ExtendedTransactionManager etm = (ExtendedTransactionManager) tmcf.getObject();
        TransactionContextManagerFactoryBean tcmfb = new TransactionContextManagerFactoryBean();
        tcmfb.setTransactionManager(etm);
        tcmfb.afterPropertiesSet();
        TransactionContextManager tcm = (TransactionContextManager) tcmfb.getObject();
        GeronimoTransactionManagerFactoryBean gtmfb = new GeronimoTransactionManagerFactoryBean();
        gtmfb.setTransactionContextManager(tcm);
        gtmfb.afterPropertiesSet();
        tm = (TransactionManager) gtmfb.getObject();
       
        broker = new BrokerService();
        broker.setPersistenceAdapter(new MemoryPersistenceAdapter());
        broker.addConnector("tcp://localhost:61616");
        broker.start();
        
        JCAFlow senderFlow = new JCAFlow();
        senderFlow.setJmsURL("tcp://localhost:61616");
        senderFlow.setTransactionContextManager(tcm);
        senderContainer.setTransactionManager(tm);
        senderContainer.setEmbedded(true);
        senderContainer.setName("senderContainer");
        senderContainer.setFlows(new Flow[] { senderFlow} );
        senderContainer.setMonitorInstallationDirectory(false);
        senderContainer.setAutoEnlistInTransaction(false);
        senderContainer.init();
        senderContainer.start();
        
        
        JCAFlow receiverFlow = new JCAFlow();
        receiverFlow.setJmsURL("tcp://localhost:61616");
        receiverFlow.setTransactionContextManager(tcm);
        receiverContainer.setTransactionManager(tm);
        receiverContainer.setEmbedded(true);
        receiverContainer.setName("receiverContainer");
        receiverContainer.setFlows(new Flow[] { receiverFlow} );
        receiverContainer.setMonitorInstallationDirectory(false);
        receiverContainer.init();
        receiverContainer.start();
    }
    
    protected void tearDown() throws Exception{
        super.tearDown();
        senderContainer.shutDown();
        receiverContainer.shutDown();
        broker.stop();
    }
    
    public void testClusteredInOut() throws Exception {
		QName service = new QName("http://org.echo", "echo");
		MyEchoComponent echoComponent = new MyEchoComponent();
		echoComponent.setService(service);
		echoComponent.setEndpoint("echo");
		ActivationSpec activationSpec = new ActivationSpec("echo", echoComponent);
		activationSpec.setService(service);
		receiverContainer.activateComponent(activationSpec);
		DefaultServiceMixClient client = new DefaultServiceMixClient(senderContainer);
		InOut inOut = client.createInOutExchange(service, null, null);
		NormalizedMessage inMessage = inOut.createMessage();
		inMessage.setContent(new StringSource("<test/>"));
		inOut.setInMessage(inMessage);
		client.sendSync(inOut, 1000);
		assertNotNull(inOut.getOutMessage());
	}
    
}