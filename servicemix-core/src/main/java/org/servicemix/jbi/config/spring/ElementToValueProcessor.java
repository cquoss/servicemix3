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
package org.servicemix.jbi.config.spring;

import org.servicemix.jbi.util.DOMUtil;
import org.springframework.beans.factory.support.BeanDefinitionReader;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * Converts an XML element into a &lt;value&gt; declaration
 *
 * @version $Revision$
 */
public class ElementToValueProcessor extends ElementProcessorSupport implements ElementProcessor {

    public ElementToValueProcessor() {
    }

    public void processElement(Element element, BeanDefinitionReader beanDefinitionReader) {
        Node root = element.getParentNode();
        root.removeChild(element);

        Element value = addElement((Element) root, "value");
        value.appendChild(value.getOwnerDocument().createTextNode(DOMUtil.getElementText(element)));
    }

}
