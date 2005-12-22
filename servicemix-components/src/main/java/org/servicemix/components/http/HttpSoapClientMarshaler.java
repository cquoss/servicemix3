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
package org.servicemix.components.http;

import javax.jbi.messaging.MessageExchange;
import javax.jbi.messaging.NormalizedMessage;
import javax.xml.transform.dom.DOMSource;

import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.StringRequestEntity;
import org.apache.xpath.CachedXPathAPI;
import org.servicemix.jbi.jaxp.StringSource;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.traversal.NodeIterator;

/**
 * A class which marshalls a client HTTP request to a NMS message
 * 
 * @version $Revision$
 */
public class HttpSoapClientMarshaler extends HttpClientMarshaler {

    public void toNMS(NormalizedMessage normalizedMessage, HttpMethod method)
            throws Exception {
        addNmsProperties(normalizedMessage, method);
        String response = method.getResponseBodyAsString();
        Node node = sourceTransformer.toDOMNode(new StringSource(response));
        CachedXPathAPI cachedXPathAPI = new CachedXPathAPI();
        NodeIterator iterator = cachedXPathAPI.selectNodeIterator(node, "/*/*[local-name()='Body']/*");
        Node root = iterator.nextNode();
        normalizedMessage.setContent(new DOMSource(root));
    }

    public void fromNMS(PostMethod method, MessageExchange exchange, NormalizedMessage normalizedMessage) throws Exception {
        addHttpHeaders(method, exchange);
        Document node = (Document) sourceTransformer.toDOMNode(normalizedMessage.getContent());
        Document document = sourceTransformer.createDocument();
        Element env = document.createElementNS("http://schemas.xmlsoap.org/soap/envelope/", "env:Envelope");
        document.appendChild(env);
        env.setAttribute("xmlns:env", "http://schemas.xmlsoap.org/soap/envelope/");
        Element body = document.createElementNS("http://schemas.xmlsoap.org/soap/envelope/", "env:Body");
        env.appendChild(body);
        body.appendChild(document.importNode(node.getDocumentElement(), true));
        String text = sourceTransformer.toString(document);
        method.setRequestEntity(new StringRequestEntity(text));
    }

}
