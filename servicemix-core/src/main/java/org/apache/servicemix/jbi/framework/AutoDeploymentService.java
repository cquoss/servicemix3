/*
 * Copyright 2005-2006 The Apache Software Foundation.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.servicemix.jbi.framework;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import javax.jbi.JBIException;
import javax.jbi.management.DeploymentException;
import javax.management.JMException;
import javax.management.MBeanAttributeInfo;
import javax.resource.spi.work.Work;
import javax.resource.spi.work.WorkException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.servicemix.jbi.config.spring.XBeanProcessor;
import org.apache.servicemix.jbi.container.EnvironmentContext;
import org.apache.servicemix.jbi.container.JBIContainer;
import org.apache.servicemix.jbi.deployment.Descriptor;
import org.apache.servicemix.jbi.deployment.ServiceAssembly;
import org.apache.servicemix.jbi.management.AttributeInfoHelper;
import org.apache.servicemix.jbi.management.BaseSystemService;
import org.apache.servicemix.jbi.util.FileUtil;
import org.apache.servicemix.jbi.util.XmlPersistenceSupport;
import org.xbean.spring.context.FileSystemXmlApplicationContext;

import edu.emory.mathcs.backport.java.util.concurrent.ConcurrentHashMap;
import edu.emory.mathcs.backport.java.util.concurrent.atomic.AtomicBoolean;
/**
 * Monitors install and deploy directories to auto install/deploy archives
 * 
 * @version $Revision$
 */
public class AutoDeploymentService extends BaseSystemService implements AutoDeploymentServiceMBean {
	
    private static final Log log = LogFactory.getLog(AutoDeploymentService.class);
    private static final String MONITOR_STATE_FILE = ".state.xml";
    private JBIContainer container;
    private EnvironmentContext environmentContext;
    protected static final String DESCRIPTOR_FILE = "META-INF/jbi.xml";
    private DeploymentService deploymentService;
    private InstallationService installationService;
    private boolean monitorInstallationDirectory = true;
    private boolean monitorDeploymentDirectory = true;
    private int monitorInterval = 10;
    private AtomicBoolean started = new AtomicBoolean(false);
    private Timer statsTimer;
    private TimerTask timerTask;
    private Map pendingSAs = new ConcurrentHashMap();
    private Map installFileMap = null;
    private Map deployFileMap = null;

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
        container.getManagementContext().registerSystemService(this, AutoDeploymentServiceMBean.class);
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
    public void updateArchive(String location, ArchiveEntry entry, boolean autoStart) throws DeploymentException{
        File tmp=AutoDeploymentService.unpackLocation(environmentContext.getTmpDir(),location);
        Descriptor root=AutoDeploymentService.buildDescriptor(tmp);
        if (root != null) {
            try{
                container.getBroker().suspend();
                if (root.getComponent() != null) {
                	String componentName = root.getComponent().getIdentification().getName();
                	entry.type = "component";
                	entry.name = componentName; 
                    log.info("Uninstalling Component: "+componentName);
                    installationService.unloadInstaller(componentName, true);
                    installationService.install(tmp, root, autoStart);
                    checkPendingSAs();
                } else if (root.getSharedLibrary() != null) {
                	String libraryName = root.getSharedLibrary().getIdentification().getName();
                	entry.type = "component";
                	entry.name = libraryName; 
                    installationService.doInstallSharedLibrary(tmp, root.getSharedLibrary());
                } else if (root.getServiceAssembly() != null) {
                    ServiceAssembly sa = root.getServiceAssembly();
                    String name = sa.getIdentification().getName();
                	entry.type = "assembly";
                	entry.name = name; 
                    try {
                        if (deploymentService.isSaDeployed(name)) {
                            deploymentService.shutDown(name);
                            deploymentService.undeploy(name);
                            deploymentService.deploy(tmp,sa);
                            if (autoStart) {
                                deploymentService.start(name);
                            }
                        } else {
                            // see if components are installed
                            entry.componentNames = deploymentService.getComponentNames(sa);
                            String missings = null;
                            boolean canDeploy = true;
                            for (Iterator it = entry.componentNames.iterator(); it.hasNext();) {
                            	String componentName = (String) it.next();
                            	if (!container.getRegistry().isLocalComponentRegistered(componentName)) {
                            		canDeploy = false;
                            		if (missings != null) {
                            			missings += ", " + componentName;
                            		} else {
                            			missings = componentName;
                            		}
                            	}
                            }
                            if (canDeploy) {
                                deploymentService.deploy(tmp, sa);
                                if (autoStart){
                                    deploymentService.start(name);
                                }
                            } else {
                            	entry.pending = true;
                                log.info("Components " + missings + " are not installed yet - adding ServiceAssembly "
                                                + name + " to pending list");
                                pendingSAs.put(tmp, entry);
                            }
                        }
                    } catch (Exception e) {
                        String errStr="Failed to update Service Assembly: "+name;
                        log.error(errStr,e);
                        throw new DeploymentException(errStr,e);
                    }
                }
            } finally {
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
    public void removeArchive(ArchiveEntry entry) throws DeploymentException{
        log.info("Attempting to remove archive at: " + entry.location);
       try{
            container.getBroker().suspend();
            if ("component".equals(entry.type)) {
                log.info("Uninstalling component: " + entry.name);
                installationService.unloadInstaller(entry.name,true);
            } 
            if ("library".equals(entry.type)) {
                log.info("Removing shared library: " + entry.name);
                installationService.uninstallSharedLibrary(entry.name);
            }
            if ("assembly".equals(entry.type)) {
                log.info("Removing service assembly " + entry.name);
                try{
                    if(deploymentService.isSaDeployed(entry.name)){
                        deploymentService.shutDown(entry.name);
                        deploymentService.undeploy(entry.name);
                    }
                } catch(Exception e) {
                    String errStr="Failed to update service assembly: "+ entry.name;
                    log.error(errStr,e);
                    throw new DeploymentException(errStr,e);
                }
            }
        } finally {
            container.getBroker().resume();
        }
    }

    /**
     * Auto deploy an SA
     * 
     * @param componentName
     */
    private void checkPendingSAs() {
    	Set deployedSas = new HashSet();
    	for (Iterator it = pendingSAs.entrySet().iterator(); it.hasNext();) {
    		Map.Entry me = (Map.Entry) it.next();
    		ArchiveEntry entry = (ArchiveEntry) me.getValue();
            boolean canDeploy = true;
            for (Iterator it2 = entry.componentNames.iterator(); it2.hasNext();) {
            	String componentName = (String) it2.next();
            	if (!container.getRegistry().isLocalComponentRegistered(componentName)) {
            		canDeploy = false;
            		break;
            	}
            }
            if (canDeploy) {
                File tmp = (File) me.getKey();
                deployedSas.add(tmp);
                try {
	                Descriptor root = AutoDeploymentService.buildDescriptor(tmp);
	                deploymentService.deploy(tmp, root.getServiceAssembly());
	                deploymentService.start(root.getServiceAssembly().getIdentification().getName());
                } catch (Exception e) {
                    String errStr = "Failed to update Service Assembly: " + tmp.getName();
                    log.error(errStr, e);
                }
            }
    	}
    	if (!deployedSas.isEmpty()) {
    		// Remove SA from pending SAs
	    	for (Iterator it = deployedSas.iterator(); it.hasNext();) {
	    		ArchiveEntry entry = (ArchiveEntry) pendingSAs.remove(it.next());
	    		entry.pending = false;
	    	}
	    	// Store new state
	    	persistState(environmentContext.getDeploymentDir(), deployFileMap);
	    	persistState(environmentContext.getInstallationDir(), installFileMap);
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
                        ArchiveEntry lastEntry = (ArchiveEntry) fileMap.get(file.getName());
                        if(lastEntry == null || file.lastModified() > lastEntry.lastModified.getTime()){
                            try{
                            	final ArchiveEntry entry = new ArchiveEntry();
                            	entry.location = file.getName();
                            	entry.lastModified = new Date(file.lastModified());
                            	fileMap.put(file.getName(), entry);
                                container.getWorkManager().doWork(new Work(){
                                    public void run(){
                                        log.info("Directory: "+root.getName()+": Archive changed: processing "
                                                        +file.getName()+" ...");
                                        try{
                                            updateArchive(file.getAbsolutePath(), entry, true);
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
                    ArchiveEntry entry = (ArchiveEntry) fileMap.remove(location);
                    try{
                        log.info("Location "+location+" no longer exists - removing ...");
                        removeArchive(entry);
                    } catch (DeploymentException e) {
                        log.error("Failed to removeArchive: "+location,e);
                    }
                }
            }
            if (!map.equals(fileMap)){
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
        if (isMonitorInstallationDirectory() && !container.isEmbedded()) {
            try {
                installFileMap=readState(environmentContext.getInstallationDir());
                removePendingEntries(installFileMap);
            } catch (Exception e) {
                log.error("Failed to read installed state", e);
            }
        }
        if (isMonitorDeploymentDirectory() && !container.isEmbedded()) {
            try {
                deployFileMap=readState(environmentContext.getDeploymentDir());
                removePendingEntries(deployFileMap);
            } catch (Exception e) {
                log.error("Failed to read deployed state", e);
            }
        }
    }
    
    private void removePendingEntries(Map map) {
        Set pendings = new HashSet();
        for (Iterator it = map.entrySet().iterator(); it.hasNext();) {
        	Map.Entry e = (Map.Entry) it.next();
        	if (((ArchiveEntry) e.getValue()).pending) {
        		pendings.add(e.getKey());
        	}
        }
        for (Iterator it = pendings.iterator(); it.hasNext();) {
        	map.remove(it.next());
        }
    }
    
    public static class ArchiveEntry {
    	public String location;
    	public Date lastModified;
    	public String type;
    	public String name;
    	public boolean pending;
    	public transient Set componentNames;
    }
}
