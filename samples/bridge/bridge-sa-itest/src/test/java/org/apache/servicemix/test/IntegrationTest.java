/**
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.servicemix.test;

import java.io.File;
import junit.framework.TestCase;
import org.apache.servicemix.jbi.util.FileUtil;
import org.apache.xbean.spring.context.ClassPathXmlApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;

/**
 * @version $Revision: 1.1 $
 */
public class IntegrationTest extends TestCase {
    protected ConfigurableApplicationContext applicationContext;

    public void testDeploy() throws Exception {
        Thread.sleep(5000);
    }

    @Override
    protected void setUp() throws Exception {
        FileUtil.deleteFile(new File("rootDir"));
        super.setUp();
        applicationContext = createApplicationContext();
        assertNotNull("Could not create the applicationContext!", applicationContext);
        applicationContext.start();
    }

    @Override
    protected void tearDown() throws Exception {
        if (applicationContext != null) {
            applicationContext.stop();
        }
        super.tearDown();
    }

    protected ConfigurableApplicationContext createApplicationContext() throws Exception {
        return new ClassPathXmlApplicationContext("integrationTest.xml");
    }
}
