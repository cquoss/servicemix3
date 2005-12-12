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
package org.servicemix.tck;

import javax.jbi.JBIException;

/**
 * A simple interface to allow polymorphic access to sending, test components.
 *
 * @version $Revision$
 */
public interface Sender {

    void sendMessages(int messageCount) throws JBIException;
    
    void sendMessages(int messageCount, boolean sync) throws JBIException;
}
