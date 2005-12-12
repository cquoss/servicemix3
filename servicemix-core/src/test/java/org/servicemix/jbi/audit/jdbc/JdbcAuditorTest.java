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
package org.servicemix.jbi.audit.jdbc;

import org.hsqldb.jdbc.jdbcDataSource;
import org.servicemix.jbi.container.JBIContainer;
import org.servicemix.jbi.jaxp.StringSource;
import org.servicemix.tck.ReceiverComponent;
import org.servicemix.tck.SenderComponent;

import javax.jbi.messaging.ExchangeStatus;
import javax.jbi.messaging.InOnly;
import javax.jbi.messaging.MessageExchange;
import javax.sql.DataSource;

import java.sql.Connection;

import junit.framework.TestCase;


public class JdbcAuditorTest extends TestCase {

    private DataSource dataSource;
    private Connection connection;
    
    
    protected void setUp() throws Exception {
        jdbcDataSource ds = new jdbcDataSource();
        ds.setDatabase("jdbc:hsqldb:mem:aname");
        ds.setUser("sa");
        dataSource = ds;
        connection = dataSource.getConnection();
    }
    
    protected void tearDown() throws Exception {
        connection.close();
    }
    
    
    public void testInsertUpdate() throws Exception {
        JBIContainer container = new JBIContainer();
        container.setFlowName("st");
        container.init();
        container.start();
        SenderComponent sender = new SenderComponent();
        ReceiverComponent receiver = new ReceiverComponent();
        container.activateComponent(sender, "sender");
        container.activateComponent(receiver, "receiver");
        
        JdbcAuditor auditor = new JdbcAuditor();
        auditor.setContainer(container);
        auditor.setDataSource(dataSource);
        auditor.afterPropertiesSet();
        
        InOnly inonly = sender.createInOnlyExchange(ReceiverComponent.SERVICE, null, null);
        inonly.setInMessage(inonly.createMessage());
        inonly.getInMessage().setContent(new StringSource("<hello>world</hello>"));
        sender.send(inonly);
        
        int nbMessages = auditor.getExchangeCount();
        assertEquals(1, nbMessages);
        MessageExchange[] exchanges = auditor.getExchanges(0, 1);
        assertNotNull(exchanges);
        assertEquals(1, exchanges.length);
        assertEquals(ExchangeStatus.DONE, exchanges[0].getStatus());
        
        auditor.resendExchange(exchanges[0]);

        nbMessages = auditor.getExchangeCount();
        assertEquals(2, nbMessages);
        MessageExchange exchange = auditor.getExchange(1);
        assertNotNull(exchange);
        assertEquals(ExchangeStatus.DONE, exchange.getStatus());
        
        /*
        PreparedStatement st = connection.prepareStatement("SELECT EXCHANGE FROM SM_AUDIT WHERE ID = ?");
        try {
            st.setString(1, inonly.getExchangeId());
            ResultSet rs = st.executeQuery();
            assertTrue(rs.next());
            byte[] data = rs.getBytes(1);
            ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(data));
            Object obj = ois.readObject();
            assertNotNull(obj);
            assertTrue(obj instanceof ExchangePacket);
            assertEquals(ExchangeStatus.DONE, ((ExchangePacket) obj).getStatus());
        } finally {
            st.close();
        }
        */
        
    }
    
}
