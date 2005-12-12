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

package org.servicemix.jbi.management.task;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.tools.ant.BuildException;
import org.servicemix.jbi.framework.FrameworkInstallationService;

import java.io.IOException;

/**
 * Install a shared library
 * @version $Revision$
 */
public class InstallSharedLibraryTask extends JbiTask {
    private static final Log log = LogFactory.getLog(InstallSharedLibraryTask.class);
    private String sharedLibURI; //sharedLibURI to install
    
    /**
     * @return Returns the sharedLibURI.
     */
    public String getSharedLibURI() {
        return sharedLibURI;
    }
    /**
     * @param sharedLibURI The sharedLibURI to set.
     */
    public void setSharedLibURI(String sharedLibURI) {
        this.sharedLibURI = sharedLibURI;
    }
    
    /**
     * execute the task
     * @throws BuildException
     */
    public void execute() throws BuildException{
        if (sharedLibURI == null){
            throw new BuildException("null sharedLibURI - sharedLibURI should be an archive");
        }
        if (sharedLibURI.endsWith(".zip") || sharedLibURI.endsWith(".jar")){
            try {
                FrameworkInstallationService is = getInstallationService();
                is.installSharedLibrary(sharedLibURI);
            }
            catch (IOException e) {
                log.error("Caught an exception getting the installation service",e);
                throw new BuildException(e);
            }
        }else {
            throw new BuildException("sharedLibURI: " + sharedLibURI + " is not an archive");
        }
    }
}