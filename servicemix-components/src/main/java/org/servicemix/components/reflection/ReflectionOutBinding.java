/** 
 * 
 * Copyright 2005 Protique Ltd
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
package org.servicemix.components.reflection;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import javax.jbi.messaging.MessageExchange;
import javax.jbi.messaging.MessagingException;
import javax.jbi.messaging.NormalizedMessage;

import org.servicemix.components.util.OutBinding;

/**
 * Consumes normalized method invocation from the JBI container.
 *
 * @version $Revision$
 */
public class ReflectionOutBinding extends OutBinding {

    private Object target;
    
    public Object getTarget() {
        return target;
    }
    public void setTarget(Object target) {
        this.target = target;
    }    
    
    // Implementation methods
    //-------------------------------------------------------------------------
    protected void process(MessageExchange messageExchange, NormalizedMessage inMessage) throws MessagingException {
        
        Method method = (Method) inMessage.getProperty("method");
        Object []args = (Object[]) inMessage.getProperty("args");
                
        try {
            method.invoke(target, args);
            done(messageExchange);
        } catch (IllegalArgumentException e) {
            throw new MessagingException(e);
        } catch (IllegalAccessException e) {
            throw new MessagingException(e);
        } catch (InvocationTargetException e) {
            throw new MessagingException(e);
        }
    }
}
