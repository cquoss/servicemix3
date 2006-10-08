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
package org.apache.servicemix.file;

import org.apache.activemq.util.IntrospectionSupport;
import org.apache.activemq.util.URISupport;
import org.apache.servicemix.common.DefaultComponent;
import org.apache.servicemix.common.Endpoint;
import org.apache.servicemix.common.ResolvedEndpoint;
import org.w3c.dom.DocumentFragment;

import javax.jbi.servicedesc.ServiceEndpoint;
import javax.xml.namespace.QName;
import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * A file based component
 *
 * @version $Revision: $
 * @org.apache.xbean.XBean element="component" description="File Component"
 */
public class FileComponent extends DefaultComponent {

    public final static String EPR_URI = "urn:servicemix:file";
    public final static QName EPR_SERVICE = new QName(EPR_URI, "FileComponent");
    public final static String EPR_NAME = "epr";

    private FileEndpoint[] endpoints;
    private FilePollingEndpoint[] pollingEndpoints;

    public FileEndpoint[] getEndpoints() {
        return endpoints;
    }

    public void setEndpoints(FileEndpoint[] endpoints) {
        this.endpoints = endpoints;
    }


    public FilePollingEndpoint[] getPollingEndpoints() {
        return pollingEndpoints;
    }

    public void setPollingEndpoints(FilePollingEndpoint[] pollingEndpoints) {
        this.pollingEndpoints = pollingEndpoints;
    }

    public ServiceEndpoint resolveEndpointReference(DocumentFragment epr) {
        return ResolvedEndpoint.resolveEndpoint(epr, EPR_URI, EPR_NAME, EPR_SERVICE, "file:");
    }

    protected List getConfiguredEndpoints() {
        List answer = new ArrayList();
        answer.addAll(asList(getEndpoints()));
        answer.addAll(asList(getPollingEndpoints()));
        return answer;
    }

    protected Class[] getEndpointClasses() {
        return new Class[]{FileEndpoint.class, FilePollingEndpoint.class};
    }

    protected QName getEPRServiceName() {
        return EPR_SERVICE;
    }

    protected Endpoint getResolvedEPR(ServiceEndpoint ep) throws Exception {
        // We receive an exchange for an EPR that has not been used yet.
        // Register a provider endpoint and restart processing.
        FileEndpoint fileEp = new FileEndpoint(this, ep);

        // TODO
        //fileEp.setRole(MessageExchange.Role.PROVIDER);

        URI uri = new URI(ep.getEndpointName());

        String file = null;
        String host = uri.getHost();
        String path = uri.getPath();
        if (host != null) {
            if (path != null) {
                // lets assume host really is a relative directory
                file = host + File.separator + path;
            }
            else {
                file = host;
            }
        }
        else {
            if (path != null) {
                file = path;
            }
            else {
                // must be an abssolute URI
                file = uri.getSchemeSpecificPart();
            }
        }

        Map map = URISupport.parseQuery(uri.getQuery());
        IntrospectionSupport.setProperties(fileEp, map);

        if (file != null) {
            fileEp.setDirectory(new File(file));
        }
        else {
            throw new IllegalArgumentException("No file defined for URL: " + uri);
        }
        fileEp.activate();
        return fileEp;
    }

}
