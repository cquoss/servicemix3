/*
 * Copyright 2005-2006 The Apache Software Foundation.
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
package org.apache.servicemix.beanflow;

import org.apache.servicemix.beanflow.Flow.Transitions;

import java.util.Timer;

import junit.framework.TestCase;

/**
 * 
 * @version $Revision: $
 */
public class TimeoutFlowTest extends TestCase {
    protected TimeoutFlow flow = new TimeoutFlow();
    protected Timer timer = new Timer();
    protected long timeout = 500;
    
    public void testFlowStopsSuccessfully() throws Exception {
        flow.getState().set(Transitions.Stopped);
        
        assertEquals("Transition", Transitions.Stopped, flow.getState().get());
        
        assertTrue("Flow should be stopped but is: " + flow.getState().get(), flow.isStopped());
        assertTrue("Flow should not have failed", !flow.isFailed());
        
        // lets sleep so that the timer can go off now to check we don't fail after we've stopped
        Thread.sleep(timeout  * 4);
        
        assertEquals("Transition", Transitions.Stopped, flow.getState().get());
        
        assertTrue("Flow should be stopped but is: " + flow.getState().get(), flow.isStopped());
        assertTrue("Flow should not have failed", !flow.isFailed());
    }

    public void testFlowTimesOutAndFails() throws Exception {
        Thread.sleep(timeout  * 4);
        
        assertEquals("Transition", Transitions.Stopped, flow.getState().get());
        
        assertTrue("Flow should be stopped but is: " + flow.getState().get(), flow.isStopped());
        assertTrue("Flow should have failed", flow.isFailed());
        
    }

    protected void setUp() throws Exception {
        timer.schedule(flow.getTimeoutTask(), timeout);
        
        assertTrue("flow should not be stopped", !flow.isStopped());
        assertTrue("flow should not have failed", !flow.isFailed());
        assertEquals("Transition", Transitions.Initialised, flow.getState().get());
        
        flow.start();
        assertEquals("Transition", Transitions.Started, flow.getState().get());
    }
}
