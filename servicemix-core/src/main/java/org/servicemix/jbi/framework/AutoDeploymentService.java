/**
 * <a href="http://servicemix.org">ServiceMix: The open source ESB</a>
 * 
 * Copyright 2005 RAJD Consultancy Ltd
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 * 
 */
package org.servicemix.jbi.framework;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import javax.jbi.JBIException;
import javax.jbi.management.DeploymentException;
import javax.jbi.management.LifeCycleMBean;
import javax.management.JMException;
import javax.management.MBeanAttributeInfo;
import javax.resource.spi.work.Work;
import javax.resource.spi.work.WorkException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.servicemix.jbi.config.spring.XBeanProcessor;
import org.servicemix.jbi.container.EnvironmentContext;
import org.servicemix.jbi.container.JBIContainer;
import org.servicemix.jbi.deployment.Descriptor;
import org.servicemix.jbi.deployment.ServiceAssembly;
import org.servicemix.jbi.management.AttributeInfoHelper;
import org.servicemix.jbi.management.BaseLifeCycle;
import org.servicemix.jbi.util.FileUtil;
import org.servicemix.jbi.util.XmlPersistenceSupport;
import org.xbean.spring.context.FileSystemXmlApplicationContext;

import edu.emory.mathcs.backport.java.util.concurrent.ConcurrentHashMap;
import edu.emory.mathcs.backport.java.util.concurrent.atomic.AtomicBoolean;
/**
 * Monitors install and deploy directories to auto install/deploy archives
 * 
 * @version $Revision$
 */
public class AutoDeploymentService extends BaseLifeCycle{
    private static final Log log=LogFactory.getLog(AutoDeploymentService.class);
    private static final String MONITOR_STATE_FILE=".state.xml";
    private JBIContainer container;
    private EnvironmentContext environmentContext;
    protected static final String DESCRIPTOR_FILE="META-INF/jbi.xml";
    private DeploymentService deploymentService;
    private InstallationService installationService;
    private boolean monitorInstallationDirectory=true;
    private boolean monitorDeploymentDirectory=true;
    private int monitorInterval=10;
    private AtomicBoolean started=new AtomicBoolean(false);
    private Timer statsTimer;
    private TimerTask timerTask;
    private Map pendingSAs=new ConcurrentHashMap();
    private Map installFileMap=null;
    private Map deployFileMap=null;

    /**
     * @return a description of this
     */
    public String getDescription(){
        return "automatically installs and deploys JBI Archives";
    }

    /**
     * @return Returns the monitorInstallationDirectory.
     */
    public boolean isMonitorInstallationDirectory(){
        return monitorInstallationDirectory;
    }

    /**
     * @param monitorInstallationDirectory
     *            The monitorInstallationDirectory to set.
     */
    public void setMonitorInstallationDirectory(boolean monitorInstallationDirectory){
        this.monitorInstallationDirectory=monitorInstallationDirectory;
    }

    /**
     * @return Returns the monitorDeploymentDirectory.
     */
    public boolean isMonitorDeploymentDirectory(){
        return monitorDeploymentDirectory;
    }

    /**
     * @param monitorDeploymentDirectory
     *            The monitorDeploymentDirectory to set.
     */
    public void setMonitorDeploymentDirectory(boolean monitorDeploymentDirectory){
        this.monitorDeploymentDirectory=monitorDeploymentDirectory;
    }

    /**
     * @return Returns the monitorInterval (number in secs)
     */
    public int getMonitorInterval(){
        return monitorInterval;
    }

    /**
     * @param monitorInterval
     *            The monitorInterval to set (in secs)
     */
    public void setMonitorInterval(int monitorInterval){
        this.monitorInterval=monitorInterval;
    }

    public void start() throws javax.jbi.JBIException{
        super.start();
        if(started.compareAndSet(false,true)){
            scheduleDirectoryTimer();
        }
    }

    /**
     * Stop the item. This suspends current messaging activities.
     * 
     * @exception javax.jbi.JBIException
     *                if the item fails to stop.
     */
    public void stop() throws javax.jbi.JBIException{
        if(started.compareAndSet(true,false)){
            super.stop();
            if(timerTask!=null){
                timerTask.cancel();
            }
        }
    }

    /**
     * Initialize the Service
     * 
     * @param container
     * @throws JBIException
     */
    public void init(JBIContainer container) throws JBIException{
        this.container=container;
        this.environmentContext=container.getEnvironmentContext();
        this.installationService=container.getInstallationService();
        this.deploymentService=container.getDeploymentService();
        //clean-up tmp directory
        if(environmentContext.getTmpDir()!=null){
            FileUtil.deleteFile(environmentContext.getTmpDir());
        }
        
        initializeFileMaps();
        container.getManagementContext().registerSystemService(this,LifeCycleMBean.class);
    }

    /**
     * Shut down the item. The releases resources, preparatory to uninstallation.
     * 
     * @exception javax.jbi.JBIException
     *                if the item fails to shut down.
     */
    public void shutDown() throws javax.jbi.JBIException{
        super.shutDown();
        stop();
        if(statsTimer!=null){
            statsTimer.cancel();
        }
        container.getManagementContext().unregisterMBean(this);
    }

    /**
     * Update an archive
     * 
     * @param location
     * @param autoStart
     * @throws DeploymentException
     */
    public void updateArchive(String location,boolean autoStart) throws DeploymentException{
        File tmp=AutoDeploymentService.unpackLocation(environmentContext.getTmpDir(),location);
        Descriptor root=AutoDeploymentService.buildDescriptor(tmp);
        if(root!=null){
            try{
                container.getBroker().suspend();
                if(root.getComponent()!=null){
                    String componentName=root.getComponent().getIdentification().getName();
                    log.info("Uninstalling Component: "+componentName);
                    installationService.unloadInstaller(componentName,true);
                    installationService.install(tmp,root,autoStart);
                    autoDeployServiceSA(componentName);
                }else if(root.getSharedLibrary()!=null){
                    installationService.doInstallSharedLibrary(tmp,root.getSharedLibrary());
                }else if(root.getServiceAssembly()!=null){
                    ServiceAssembly sa=root.getServiceAssembly();
                    String name=sa.getIdentification().getName();
                    try{
                        if(deploymentService.isSaDeployed(name)){
                            deploymentService.shutDown(name);
                            deploymentService.undeploy(name);
                            deploymentService.deploy(tmp,sa);
                            if(autoStart){
                                deploymentService.start(name);
                            }
                        }else{
                            // see if Component is installed
                            String componentName=deploymentService.getComponentName(sa);
                            if(container.getRegistry().isLocalComponentRegistered(componentName)){
                                deploymentService.deploy(tmp,sa);
                                if(autoStart){
                                    deploymentService.start(name);
                                }
                            }else{
                                log.info("No Component named "+componentName+" exists yet - adding ServiceAssembly "
                                                +name+" to pending list");
                                pendingSAs.put(componentName,tmp);
                            }
                        }
                    }catch(Exception e){
                        String errStr="Failed to update Service Assembly: "+name;
                        log.error(errStr,e);
                        throw new DeploymentException(errStr,e);
                    }
                }
            }finally{
                container.getBroker().resume();
            }
        }
    }

    /**
     * Remove an archive location
     * 
     * @param location
     * @throws DeploymentException
     */
    public void removeArchive(String location) throws DeploymentException{
        log.info("Attempting to remove archive at: "+location);
        File tmp=AutoDeploymentService.unpackLocation(environmentContext.getTmpDir(),location);
        Descriptor root=AutoDeploymentService.buildDescriptor(tmp);
        if(root!=null){
            try{
                container.getBroker().suspend();
                if(root.getComponent()!=null){
                    String componentName=root.getComponent().getIdentification().getName();
                    log.info("Uninstalling Component: "+componentName);
                    installationService.unloadInstaller(componentName,true);
                }
                if(root.getSharedLibrary()!=null){
                    String name=root.getSharedLibrary().getIdentification().getName();
                    log.info("removing shared library: "+name);
                    installationService.uninstallSharedLibrary(name);
                }
                if(root.getServiceAssembly()!=null){
                    ServiceAssembly sa=root.getServiceAssembly();
                    String name=sa.getIdentification().getName();
                    log.info("removing service assembly "+name);
                    try{
                        if(deploymentService.isSaDeployed(name)){
                            deploymentService.shutDown(name);
                            deploymentService.undeploy(name);
                        }
                    }catch(Exception e){
                        String errStr="Failed to update Service Assembly: "+name;
                        log.error(errStr,e);
                        throw new DeploymentException(errStr,e);
                    }
                }
            }finally{
                container.getBroker().resume();
            }
        }else{
            throw new DeploymentException("remove archive  cannot find descriptor for "+location);
        }
    }

    /**
     * Auto deploy an SA
     * 
     * @param componentName
     */
    private void autoDeployServiceSA(String componentName){
        File tmpDir=(File) pendingSAs.remove(componentName);
        if(tmpDir!=null){
            try{
                Descriptor root=AutoDeploymentService.buildDescriptor(tmpDir);
                if(root!=null){
                    ServiceAssembly sa=root.getServiceAssembly();
                    if(sa!=null&&sa.getIdentification()!=null){
                        String name=sa.getIdentification().getName();
                        log.info("auto deploying Service Assembly: "+name);
                        if(!deploymentService.isSaDeployed(name)){
                            deploymentService.deploy(tmpDir,sa);
                            deploymentService.start(name);
                        }
                    }else{
                        log.warn("Could not find ServiceAssembly in descriptor from "+tmpDir);
                    }
                }else{
                    log.warn("Failed to build descriptor from "+tmpDir);
                }
            }catch(Exception e){
                log.error("Failed to deploy service assembly to "+componentName,e);
            }
        }
    }

    /**
     * Check to see if an archive is installed
     * 
     * @param location
     * @return the Descriptor if installed
     */
    protected Descriptor getInstalledDescriptor(String location) throws JBIException{
        Descriptor result=null;
        boolean exists=false;
        try{
            File tmp=AutoDeploymentService.unpackLocation(environmentContext.getTmpDir(),location);
            Descriptor root=AutoDeploymentService.buildDescriptor(tmp);
            if(root.getComponent()!=null){
                String componentName=root.getComponent().getIdentification().getName();
                exists=container.getRegistry().isLocalComponentRegistered(componentName);
            }else if(root.getServiceAssembly()!=null){
                ServiceAssembly sa=root.getServiceAssembly();
                String name=sa.getIdentification().getName();
                exists=deploymentService.isSaDeployed(name);
            }else if(root.getSharedLibrary()!=null){
                String name=root.getSharedLibrary().getIdentification().getName();
                exists=installationService.containsSharedLibrary(name);
            }
            if(exists){
                result=root;
            }else{
                FileUtil.deleteFile(tmp);
            }
        }catch(DeploymentException e){
            result=null;
            log.error("Could not process "+location,e);
        }
        return result;
    }

    /**
     * Get an array of MBeanAttributeInfo
     * 
     * @return array of AttributeInfos
     * @throws JMException
     */
    public MBeanAttributeInfo[] getAttributeInfos() throws JMException{
        AttributeInfoHelper helper=new AttributeInfoHelper();
        helper.addAttribute(getObjectToManage(),"monitorInstallationDirectory",
                        "Periodically monitor the Installation directory");
        helper.addAttribute(getObjectToManage(),"monitorInterval","Interval (secs) before monitoring");
        return AttributeInfoHelper.join(super.getAttributeInfos(),helper.getAttributeInfos());
    }

    /**
     * Build a Descriptor from a file archieve
     * 
     * @param tmpDir
     * @return the Descriptor object
     */
    protected static Descriptor buildDescriptor(File tmpDir){
        Descriptor root=null;
        File descriptorFile=new File(tmpDir,DESCRIPTOR_FILE);
        if(descriptorFile.exists()){
            ClassLoader cl=Thread.currentThread().getContextClassLoader();
            try{
                Thread.currentThread().setContextClassLoader(AutoDeploymentService.class.getClassLoader());
                FileSystemXmlApplicationContext context=new FileSystemXmlApplicationContext("file:///"
                                +descriptorFile.getAbsolutePath(),Arrays.asList(new Object[] { new XBeanProcessor() }));
                root=(Descriptor) context.getBean("jbi");
            }finally{
                Thread.currentThread().setContextClassLoader(cl);
            }
        }
        return root;
    }

    /**
     * Unpack a location into a temp file directory
     * 
     * @param location
     * @return tmp directory
     * @throws DeploymentException
     */
    protected static File unpackLocation(File tmpRoot,String location) throws DeploymentException{
        File tmpDir=null;
        try{
            File file=new File(location);
            if(file.isDirectory()){
                log.info("Deploying an exploded jar/zip,  we will create a temporary jar for it.");
                // If we have a directory then we should move it over
                File newFile=new File(tmpRoot.getAbsolutePath()+"/exploded.jar");
                newFile.delete();
                FileUtil.zipDir(file.getAbsolutePath(),newFile.getAbsolutePath());
                file=newFile;
                log.info("Deployment will now work from "+file.getAbsolutePath());
            }
            if(!file.exists()){
                // assume it's a URL
                try{
                    URL url=new URL(location);
                    String fileName=url.getFile();
                    if(fileName==null||(!fileName.endsWith(".zip")&&!fileName.endsWith(".jar"))){
                        throw new DeploymentException("Location: "+location+" is not an archive");
                    }
                    file=FileUtil.unpackArchive(url,tmpRoot);
                }catch(MalformedURLException e){
                    throw new DeploymentException(e);
                }
            }
            if(FileUtil.archiveContainsEntry(file,AutoDeploymentService.DESCRIPTOR_FILE)){
                tmpDir=FileUtil.createUniqueDirectory(tmpRoot,file.getName());
                FileUtil.unpackArchive(file,tmpDir);
                log.info("Unpacked archive "+location+" to "+tmpDir);
            }
        }catch(IOException e){
            log.error("I/O error installing archive",e);
            throw new DeploymentException(e);
        }
        return tmpDir;
    }

    private void scheduleDirectoryTimer(){
        if(!container.isEmbedded()&&(isMonitorInstallationDirectory()||isMonitorDeploymentDirectory())){
            if(statsTimer==null){
                statsTimer=new Timer(true);
            }
            if(timerTask!=null){
                timerTask.cancel();
            }
            timerTask=new TimerTask(){
                public void run(){
                    if(isMonitorInstallationDirectory()){
                        monitorDirectory(environmentContext.getInstallationDir(),installFileMap);
                    }
                    if(isMonitorDeploymentDirectory()){
                        monitorDirectory(environmentContext.getDeploymentDir(),deployFileMap);
                    }
                }
            };
            long interval=monitorInterval*1000;
            statsTimer.scheduleAtFixedRate(timerTask,0,interval);
        }
    }


    private void monitorDirectory(final File root,final Map fileMap){
        if(root!=null)
            log.debug("Monitoring directory "+root.getAbsolutePath()+" for new or modified archives");
        else
            log.debug("No directory to monitor for new or modified archives for "+""
                            +((fileMap==installFileMap)?"Installation":"Deployment")+".");
        List tmpList=new ArrayList();
        if(root!=null&&root.exists()&&root.isDirectory()){
            File[] files=root.listFiles();
            if(files!=null){
                for(int i=0;i<files.length;i++){
                    final File file=files[i];
                    tmpList.add(file.getName());
                    if(file.getPath().endsWith(".jar")||file.getPath().endsWith(".zip")){
                        Date lastModified=(Date) fileMap.get(file.getName());
                        if(lastModified==null||file.lastModified()>lastModified.getTime()){
                            try{
                                container.getWorkManager().doWork(new Work(){
                                    public void run(){
                                        log.info("Directory: "+root.getName()+": Archive changed: processing "
                                                        +file.getName()+" ...");
                                        try{
                                            updateArchive(file.getAbsolutePath(),true);
                                            fileMap.put(file.getName(),new Date(file.lastModified()));
                                        }catch(Exception e){
                                            log.warn("Directory: "+root.getName()+": Automatic install of "+file
                                                            +" failed",e);
                                        }
                                        log.info("Directory: "+root.getName()+": Finished installation of archive:  "
                                                        +file.getName());
                                    }

                                    public void release(){}
                                });
                            }catch(WorkException e){
                                log.warn("Automatic install of "+file+" failed",e);
                            }finally{
                                persistState(root, fileMap);
                            }
                        }
                    }
                }
            }
            // now remove any locations no longer here
            Map map=new HashMap(fileMap);
            for(Iterator i=map.keySet().iterator();i.hasNext();){
                String location=i.next().toString();
                if(!tmpList.contains(location)){
                    fileMap.remove(location);
                    try{
                        log.info("Location "+location+" no longer exists - removing ...");
                        removeArchive(location);
                    }catch(DeploymentException e){
                        log.error("Failed to removeArchive: "+location,e);
                    }
                }
            }
            if (map.equals(fileMap)){
                persistState(root, fileMap);
            }
        }
    }

    private void persistState(File root,Map map){
        try{
            File file = FileUtil.getDirectoryPath(root,MONITOR_STATE_FILE);
            XmlPersistenceSupport.write(file, map);
        }catch(IOException e){
            log.error("Failed to persist file state to: "+root,e);
        }
    }

    private Map readState(File root){
        Map result = new HashMap();
        try{
            File file = FileUtil.getDirectoryPath(root,MONITOR_STATE_FILE);
            if(file.exists()) {
                result = (Map) XmlPersistenceSupport.read(file);
            }else {
                log.debug("State file doesn't exist: " + file.getPath());
            }
        }catch(Exception e){
            log.error("Failed to read file state from: "+root,e);
        }
        return result;
    }

    private void initializeFileMaps(){
        if(isMonitorInstallationDirectory()  && !container.isEmbedded()){
            try{
                installFileMap=readState(environmentContext.getInstallationDir());
            }catch(Exception e){
                log.error("Failed to read installed state",e);
            }
        }
        if(isMonitorDeploymentDirectory() && !container.isEmbedded()){
            try{
                deployFileMap=readState(environmentContext.getDeploymentDir());
            }catch(Exception e){
                log.error("Failed to read deployed state",e);
            }
        }
    }
}
