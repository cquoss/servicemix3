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
package org.apache.servicemix.jbi.installation;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FilterOutputStream;
import java.io.InputStream;
import java.util.jar.JarOutputStream;
import java.util.jar.Manifest;
import java.util.zip.ZipEntry;

import javax.jbi.management.AdminServiceMBean;
import javax.jbi.management.DeploymentServiceMBean;
import javax.jbi.management.InstallationServiceMBean;
import javax.xml.stream.XMLOutputFactory;
import javax.xml.stream.XMLStreamWriter;

import junit.framework.TestCase;

import org.apache.servicemix.jbi.container.JBIContainer;
import org.apache.servicemix.jbi.util.FileUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public abstract class AbstractManagementTest extends TestCase {

    protected static final transient Logger LOGGER = LoggerFactory.getLogger(AbstractManagementTest.class);

    protected JBIContainer container;

    /*
     * @see TestCase#setUp()
     */
    protected void setUp() throws Exception {
        super.setUp();
    }

    /*
     * @see TestCase#tearDown()
     */
    protected void tearDown() throws Exception {
        super.tearDown();
        try {
            shutdownContainer();
        } catch (Exception e) {
            LOGGER.info("Error shutting down container", e);
        }
    }

    protected void startContainer(boolean clean) throws Exception {
        shutdownContainer();
        if (clean) {
            Thread.sleep(1000);
            assertTrue(FileUtil.deleteFile(new File("target/testWDR")));
        }
        container = new JBIContainer();
        container.setRootDir("target/testWDR");
        initContainer();
        container.init();
        container.start();
    }

    protected void initContainer() {
        container.setCreateMBeanServer(true);
        container.setMonitorInstallationDirectory(false);
        container.setMonitorDeploymentDirectory(false);
    }

    protected void shutdownContainer() throws Exception {
        if (container != null) {
            container.shutDown();
        }
    }

    protected JBIContainer getContainer() {
        return container;
    }

    protected File createInstallerArchive(String jbi) throws Exception {
        InputStream is = getClass().getResourceAsStream(jbi + "-jbi.xml");
        File jar = File.createTempFile("jbi", ".zip");
        JarOutputStream jos = new JarOutputStream(new FileOutputStream(jar));
        jos.putNextEntry(new ZipEntry("META-INF/jbi.xml"));
        byte[] buffer = new byte[is.available()];
        is.read(buffer);
        jos.write(buffer);
        jos.closeEntry();
        jos.close();
        is.close();
        return jar;
    }

    protected File createDummyArchive() throws Exception {
        File jar = File.createTempFile("jbi", ".zip");
        JarOutputStream jos = new JarOutputStream(new FileOutputStream(jar));
        jos.putNextEntry(new ZipEntry("test.txt"));
        jos.closeEntry();
        jos.close();
        return jar;
    }

    protected File createServiceAssemblyArchive(String saName, String suName, String compName) throws Exception {
        return createServiceAssemblyArchive(saName, new String[] {suName }, new String[] {compName });
    }

    protected File createServiceAssemblyArchive(String saName, String[] suName, String[] compName) throws Exception {
        File jar = File.createTempFile("jbi", ".zip");
        JarOutputStream jos = new JarOutputStream(new FileOutputStream(jar));
        // Write jbi.xml
        jos.putNextEntry(new ZipEntry("META-INF/jbi.xml"));
        XMLOutputFactory xof = XMLOutputFactory.newInstance();
        // xof.setProperty(XMLOutputFactory.IS_REPAIRING_NAMESPACES,
        // Boolean.TRUE);
        XMLStreamWriter xsw = xof.createXMLStreamWriter(new FilterOutputStream(jos) {
            public void close() {
            }
        });
        xsw.writeStartDocument();
        xsw.writeStartElement("jbi");
        xsw.writeAttribute("xmlns", "http://java.sun.com/xml/ns/jbi");
        xsw.writeAttribute("version", "1.0");
        xsw.writeStartElement("service-assembly");
        xsw.writeStartElement("identification");
        xsw.writeStartElement("name");
        xsw.writeCharacters(saName);
        xsw.writeEndElement();
        xsw.writeEndElement();
        for (int i = 0; i < suName.length; i++) {
            xsw.writeStartElement("service-unit");
            xsw.writeStartElement("identification");
            xsw.writeStartElement("name");
            xsw.writeCharacters(suName[i]);
            xsw.writeEndElement();
            xsw.writeEndElement();
            xsw.writeStartElement("target");
            xsw.writeStartElement("artifacts-zip");
            xsw.writeCharacters(suName[i] + ".zip");
            xsw.writeEndElement();
            xsw.writeStartElement("component-name");
            xsw.writeCharacters(compName[i]);
            xsw.writeEndElement();
            xsw.writeEndElement();
            xsw.writeEndElement();
        }
        xsw.writeEndElement();
        xsw.writeEndElement();
        xsw.writeEndDocument();
        xsw.flush();
        jos.closeEntry();
        // Put su artifact
        for (int i = 0; i < suName.length; i++) {
            jos.putNextEntry(new ZipEntry(suName[i] + ".zip"));
            JarOutputStream jos2 = new JarOutputStream(jos, new Manifest());
            jos2.finish();
            jos2.flush();
            jos.closeEntry();
        }
        // Close jar
        jos.close();
        return jar;
    }

    protected InstallationServiceMBean getInstallationService() throws Exception {
        return container.getInstallationService();
    }

    protected DeploymentServiceMBean getDeploymentService() throws Exception {
        return container.getDeploymentService();
    }

    protected AdminServiceMBean getAdminService() throws Exception {
        return container.getManagementContext();
    }

}
