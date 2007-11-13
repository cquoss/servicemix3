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
package org.apache.servicemix;

import java.io.IOException;
import java.io.File;
import java.io.FileInputStream;
import java.util.Properties;

import org.apache.servicemix.nmr.api.Endpoint;
import org.apache.servicemix.nmr.api.NMR;
import org.osgi.framework.BundleContext;
import org.osgi.framework.ServiceReference;
import org.springframework.osgi.test.AbstractConfigurableBundleCreatorTests;

public class IntegrationTest extends AbstractConfigurableBundleCreatorTests {

    private Properties dependencies;

    /**
	 * The manifest to use for the "virtual bundle" created
	 * out of the test classes and resources in this project
	 * 
	 * This is actually the boilerplate manifest with one additional
	 * import-package added. We should provide a simpler customization
	 * point for such use cases that doesn't require duplication
	 * of the entire manifest...
	 */
	protected String getManifestLocation() { 
		return "classpath:org/apache/servicemix/MANIFEST.MF";
	}
	
	/**
	 * The location of the packaged OSGi bundles to be installed
	 * for this test. Values are Spring resource paths. The bundles
	 * we want to use are part of the same multi-project maven
	 * build as this project is. Hence we use the localMavenArtifact
	 * helper method to find the bundles produced by the package
	 * phase of the maven build (these tests will run after the
	 * packaging phase, in the integration-test phase). 
	 * 
	 * JUnit, commons-logging, spring-core and the spring OSGi
	 * test bundle are automatically included so do not need
	 * to be specified here.
	 */
	protected String[] getTestBundlesNames() {
        return new String[] {
            getBundle("org.apache.geronimo.specs", "geronimo-jms_1.1_spec"),
            getBundle("org.apache.geronimo.specs", "geronimo-servlet_2.5_spec"),
            getBundle("org.apache.geronimo.specs", "geronimo-j2ee-management_1.1_spec"),
            getBundle("org.apache.geronimo.specs", "geronimo-stax-api_1.0_spec"),
            getBundle("org.apache.geronimo.specs", "geronimo-activation_1.1_spec"),
            getBundle("org.apache.felix", "org.osgi.compendium"),
            getBundle("org.ops4j.pax.logging", "pax-logging-api"),
            getBundle("org.ops4j.pax.logging", "pax-logging-service"),
            getBundle("org.apache.servicemix.bundles", "org.apache.servicemix.bundles.aopalliance"),
            getBundle("org.apache.servicemix.bundles", "org.apache.servicemix.bundles.jaxb-api"),
            getBundle("org.apache.servicemix.bundles", "org.apache.servicemix.bundles.jaxb-impl"),
            getBundle("org.apache.servicemix.bundles", "org.apache.servicemix.bundles.httpcore"),
            getBundle("org.apache.activemq", "activemq-core"),
            getBundle("org.springframework", "spring-beans"),
            getBundle("org.springframework", "spring-core"),
            getBundle("org.springframework", "spring-context"),
            getBundle("org.springframework", "spring-aop"),
            getBundle("org.springframework", "spring-test"),
            getBundle("org.springframework", "spring-tx"),
            getBundle("org.springframework", "spring-jms"),
            getBundle("org.springframework.osgi", "spring-osgi-core"),
            getBundle("org.springframework.osgi", "spring-osgi-io"),
            getBundle("org.springframework.osgi", "spring-osgi-extender"),
            getBundle("org.springframework.osgi", "spring-osgi-test"),
            getBundle("org.springframework.osgi", "spring-osgi-annotation"),
            getBundle("org.springframework.osgi", "junit.osgi"),
            getBundle("org.springframework.osgi", "asm.osgi"),
            getBundle("org.apache.camel", "camel-core"),
            getBundle("org.apache.camel", "camel-spring"),
            getBundle("org.apache.camel", "camel-osgi"),
            getBundle("org.apache.camel", "camel-jms"),
            getBundle("org.apache.camel", "camel-jhc"),
            getBundle("org.apache.servicemix.jbi", "org.apache.servicemix.jbi.api"),
            getBundle("org.apache.servicemix.nmr", "org.apache.servicemix.nmr.api"),
            getBundle("org.apache.servicemix.nmr", "org.apache.servicemix.nmr.core"),
			getBundle("org.apache.servicemix.nmr", "org.apache.servicemix.nmr.spring"),
            getBundle("org.apache.servicemix.nmr", "org.apache.servicemix.nmr.osgi"),
            getBundle("org.apache.servicemix", "org.apache.servicemix.camel"),
            getBundle("org.apache.servicemix", "org.apache.servicemix.jaxws"),
            getBundle("org.apache.servicemix.examples", "org.apache.servicemix.examples.intermediary"),
            getBundle("org.apache.servicemix.examples", "org.apache.servicemix.examples.jaxws"),
		};
	}

    protected String getBundle(String groupId, String artifactId) {
        return groupId + "," + artifactId + "," + getBundleVersion(groupId, artifactId);
    }

    protected String getBundleVersion(String groupId, String artifactId) {
        if (dependencies == null) {
            try {
                File f = new File(System.getProperty("basedir"), "target/classes/META-INF/maven/dependencies.properties");
                Properties prop = new Properties();
                prop.load(new FileInputStream(f));
                dependencies = prop;
            } catch (IOException e) {
                throw new IllegalStateException("Unable to load dependencies informations", e);
            }
        }
        String version = dependencies.getProperty(groupId + "/" + artifactId + "/version");
        if (version == null) {
            throw new IllegalStateException("Unable to find dependency information for: " + groupId + "/" + artifactId + "/version");
        }
        return version;
    }

    protected String[] getTestFrameworkBundlesNames() {
        return null;
    }

    /**
	 * The superclass provides us access to the root bundle
	 * context via the 'getBundleContext' operation
	 */
	public void testOSGiStartedOk() {
		assertNotNull(bundleContext);
	}
	
	/**
	 * The simple service should have been exported as an
	 * OSGi service, which we can verify using the OSGi
	 * service APIs.
	 *
	 * In a Spring bundle, using osgi:reference is a much
	 * easier way to get a reference to a published service.
	 * 
	 */
	public void testSimpleServiceExported() {
		waitOnContextCreation("org.apache.servicemix.examples.intermediary");
        ServiceReference ref = bundleContext.getServiceReference(NMR.class.getName());
        ServiceReference endpointRef = bundleContext.getServiceReference(Endpoint.class.getName());
        assertNotNull("Service Reference is null", ref);
        assertNotNull("Endpoint Reference is null", endpointRef);
        try {
            NMR nmr = (NMR) bundleContext.getService(ref);
            assertNotNull("Cannot find the service", nmr);
            Endpoint jaxwsProvider = (Endpoint) bundleContext.getService(endpointRef);
            assertNotNull(jaxwsProvider);
            assertEquals(jaxwsProvider.getClass().getName(), 
            		"org.apache.servicemix.jaxws.JAXWSProvider");
        } finally {
            bundleContext.ungetService(ref);
        }
	}

}