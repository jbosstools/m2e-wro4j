/*******************************************************************************
 * Copyright (c) 2012-2013 Red Hat, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.jboss.tools.m2e.wro4j.internal;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.maven.plugin.MojoExecution;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.Scanner;
import org.codehaus.plexus.util.xml.Xpp3Dom;
import org.codehaus.plexus.util.xml.Xpp3DomUtils;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.embedder.IMaven;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.core.project.MavenProjectUtils;
import org.eclipse.m2e.core.project.configurator.MojoExecutionBuildParticipant;
import org.eclipse.osgi.util.NLS;
import org.sonatype.plexus.build.incremental.BuildContext;
import org.sonatype.plexus.build.incremental.ThreadBuildContext;

/**
 * m2e build participant for wro4j-maven-plugin
 * 
 * @author Fred Bricon
 */
public class Wro4jBuildParticipant extends MojoExecutionBuildParticipant {

  private static final String M2E_WRO4J_WTP_INTEGRATION_KEY = "m2e.wro4j.wtp.integration";

  private static final String CONTEXT_FOLDER = "contextFolder";

  private static final String TOKEN_SEPARATOR = ",\\s*";
	
  private static final Pattern WRO4J_FILES_PATTERN = Pattern.compile("^(\\/?.*\\/)?wro\\.(xml|groovy|properties)$");
	
  private static final Pattern WEB_RESOURCES_PATTERN = Pattern.compile("([^\\s]+(\\.(?i)(js|css|scss|sass|less|coffee|json|template))$)");
  
  private static final String DESTINATION_FOLDER = "destinationFolder";
  private static final String CSS_DESTINATION_FOLDER = "cssDestinationFolder";
  private static final String JS_DESTINATION_FOLDER = "jsDestinationFolder";
  private static final String GROUP_NAME_MAPPING_FILE = "groupNameMappingFile";

  private BuildContext currentBuildContext;

  public Wro4jBuildParticipant(MojoExecution execution) {
    super(execution, true);
  }

  @SuppressWarnings("unchecked")
  @Override
  public Set<IProject> build(int kind, IProgressMonitor monitor)
      throws Exception {

    MojoExecution mojoExecution = getMojoExecution();
    if (mojoExecution == null) {
      return null;
    }

    BuildContext originalBuildContext = super.getBuildContext();
    currentBuildContext = originalBuildContext;
    IMavenProjectFacade facade = getMavenProjectFacade();
    Collection<File> sources = getContextRoots(facade, mojoExecution, monitor);
    if (notCleanFullBuild(kind)) {
    	Collection<String> includedFiles = new ArrayList<String>();
	    // check if any of the web resource files changed
	    for (File source : sources){
	    	// TODO also analyze output classes folders as wro4j can use classpath files
	    	Scanner ds = currentBuildContext.newScanner(source); // delta or full scanner
	    	ds.scan();
	    	includedFiles.addAll(Arrays.asList(ds.getIncludedFiles()));
	    }
    	if (isPomModified() || interestingFileChangeDetected(includedFiles, WRO4J_FILES_PATTERN)) {
    		//treat as new full build as wro4j only checks for classic resources changes during    incremental builds
			currentBuildContext = new CleanBuildContext(originalBuildContext);
    	} else if (!interestingFileChangeDetected(includedFiles, WEB_RESOURCES_PATTERN)) {
    		return null;
    	}
    }

    Xpp3Dom originalConfiguration = mojoExecution.getConfiguration();

    Set<IProject> result = null;
    try {
      MavenProject mavenProject = facade.getMavenProject(monitor);
      File destinationFolder = getLocation(mavenProject, mojoExecution, DESTINATION_FOLDER, monitor);
      File jsDestinationFolder = getLocation(mavenProject, mojoExecution, JS_DESTINATION_FOLDER, monitor);
      File cssDestinationFolder = getLocation(mavenProject, mojoExecution, CSS_DESTINATION_FOLDER, monitor);
      File groupNameMappingFile = getLocation(mavenProject, mojoExecution, GROUP_NAME_MAPPING_FILE, monitor);

      Xpp3Dom customConfiguration = customize(originalConfiguration, 
    		                                  sources,
                                              destinationFolder, 
                                              jsDestinationFolder, 
                                              cssDestinationFolder,
                                              groupNameMappingFile);

      // Add custom configuration
      mojoExecution.setConfiguration(customConfiguration);

      if (monitor != null) {
    	  String taskName = NLS.bind("Invoking {0} on {1}", getMojoExecution().getMojoDescriptor().getFullGoalName()
  														, getMavenProjectFacade().getProject().getName());
    	  monitor.setTaskName(taskName);
      }
      // execute mojo
      ThreadBuildContext.setThreadBuildContext(currentBuildContext);

      result = super.build(kind, monitor);

      // tell m2e builder to refresh generated resources on original build context
      refreshWorkspace(mavenProject, mojoExecution, originalBuildContext, monitor);

    } finally {
      // restore original configuration
      mojoExecution.setConfiguration(originalConfiguration);
      ThreadBuildContext.setThreadBuildContext(originalBuildContext);
    }

    return result;
  }

  private Collection<File> getContextRoots(IMavenProjectFacade facade, MojoExecution mojoExecution, IProgressMonitor monitor)
      throws CoreException {
	
    IMaven maven = MavenPlugin.getMaven();
    String contextRoots = maven.getMojoParameterValue(facade.getMavenProject(monitor), mojoExecution, CONTEXT_FOLDER, String.class, monitor);
	List<File> locations = new ArrayList<File>();
    if (contextRoots != null) {
    	String[] crs = contextRoots.split(TOKEN_SEPARATOR);
    	IPath root = facade.getProject().getLocation();
    	for (String cr : crs) {
    		String location = cr.trim();
    		if (!location.isEmpty()) {
    			File l = new File(location);
	    		if (l.isAbsolute()) {
	    			locations.add(l);
	    		} else {
					locations.add(root.append(location).toFile());
	    		}
	    		
    			
    		}
    	}; 
    }
    if (locations.isEmpty()) {
    	locations.add(new File("src/main/webapp"));
    }
    return locations;
  }


  private File getLocation(MavenProject mavenProject, MojoExecution mojoExecution, String parameterName, IProgressMonitor monitor)
      throws CoreException {
    IMaven maven = MavenPlugin.getMaven();
    File location = maven.getMojoParameterValue(mavenProject, mojoExecution, parameterName, File.class, monitor);
    return location;
  }


  
  private boolean interestingFileChangeDetected(Collection<String> includedFiles, Pattern pattern) throws CoreException {
    if (includedFiles == null || includedFiles.isEmpty()) {
    	return false;
    }
    for (String file : includedFiles) {
      String portableFile = file.replace('\\', '/');
      Matcher m = pattern.matcher(portableFile);
      if (m.matches()) {
    	  return true;
      }
    }

    return false;
  }

    
  private boolean isPomModified() {
    IMavenProjectFacade facade = getMavenProjectFacade();
    IResourceDelta delta = getDelta(facade.getProject());
    if (delta == null) {
      return false;
    }

    if (delta.findMember(facade.getPom().getProjectRelativePath()) != null) {
      return true;
    }
    return false;
  }

  private void refreshWorkspace(MavenProject mavenProject, MojoExecution mojoExecution, BuildContext buildContext, IProgressMonitor monitor) throws CoreException {
    refreshResource(mavenProject, mojoExecution, buildContext, DESTINATION_FOLDER, monitor);
    refreshResource(mavenProject, mojoExecution, buildContext, CSS_DESTINATION_FOLDER, monitor);
    refreshResource(mavenProject, mojoExecution, buildContext, JS_DESTINATION_FOLDER, monitor);
    refreshResource(mavenProject, mojoExecution, buildContext, GROUP_NAME_MAPPING_FILE, monitor);
  }

  private void refreshResource(MavenProject mavenProject, MojoExecution mojoExecution, BuildContext buildContext, String parameterName, IProgressMonitor monitor) throws CoreException {
    File location = getLocation(mavenProject, mojoExecution, parameterName, monitor);
    if (location != null && location.exists()) {
      buildContext.refresh(location);
    }
  }

  private Xpp3Dom customize(Xpp3Dom originalConfiguration, Collection<File> contextFolders,
      File originalDestinationFolder, File originalJsDestinationFolder,
      File originalCssDestinationFolder, File originalGroupNameMappingFile) throws IOException, CoreException {
    IMavenProjectFacade facade = getMavenProjectFacade();
    if (!"war".equals(facade.getPackaging())) {
      // Not a war project, we don't know how to customize that
      return originalConfiguration;
    }
    Xpp3Dom customConfiguration = new Xpp3Dom("configuration");
    Xpp3DomUtils.mergeXpp3Dom(customConfiguration, originalConfiguration);

    IProject project = facade.getProject();
    String target = facade.getMavenProject().getBuild().getDirectory();
    IPath relativeTargetPath = MavenProjectUtils.getProjectRelativePath(project, target);
    if (relativeTargetPath == null) {
      // target folder not under the project directory, we bail
      return customConfiguration;
    }

    IFolder m2eWtpFolder = project.getFolder(relativeTargetPath.append("m2e-wtp"));
     
    if (!m2eWtpFolder.exists() || isWtpIntegrationDisabled(facade.getMavenProject(new NullProgressMonitor()))) {
      // Not a m2e-wtp project, we don't know how to customize either
      // TODO Try to support Sonatype's webby instead?
      return customConfiguration;
    }
    IFolder webResourcesFolder = m2eWtpFolder.getFolder("web-resources");

    IPath fullTargetPath = new Path(target);
    IPath defaultOutputPathPrefix = fullTargetPath.append(facade.getMavenProject().getBuild().getFinalName());

    fixContextFolders(customConfiguration, contextFolders);

    customizeLocation(originalDestinationFolder, webResourcesFolder,
        defaultOutputPathPrefix, customConfiguration, DESTINATION_FOLDER);

    customizeLocation(originalJsDestinationFolder, webResourcesFolder,
        defaultOutputPathPrefix, customConfiguration, JS_DESTINATION_FOLDER);

    customizeLocation(originalCssDestinationFolder, webResourcesFolder,
        defaultOutputPathPrefix, customConfiguration, CSS_DESTINATION_FOLDER);

    customizeLocation(originalGroupNameMappingFile, webResourcesFolder,
            defaultOutputPathPrefix, customConfiguration, GROUP_NAME_MAPPING_FILE);

    return customConfiguration;
  }

  private boolean isWtpIntegrationDisabled(MavenProject mavenProject) {
	Properties properties = mavenProject.getProperties();
	String isWtpIntegrationProperty = properties.getProperty(M2E_WRO4J_WTP_INTEGRATION_KEY, Boolean.TRUE.toString());
	return !Boolean.parseBoolean(isWtpIntegrationProperty);
  }

  private void customizeLocation(File originalDestinationFolder,
      IFolder webResourcesFolder, IPath defaultOutputPathPrefix,
      Xpp3Dom configuration, String parameterName) throws IOException {

    if (originalDestinationFolder != null) {
      IPath customPath = getReplacementPath(originalDestinationFolder, webResourcesFolder, defaultOutputPathPrefix);
      if (customPath != null) {
        Xpp3Dom dom = configuration.getChild(parameterName);
        if (dom == null) {
          dom = new Xpp3Dom(parameterName);
          configuration.addChild(dom);
        }
        dom.setValue(customPath.toOSString());
      }
    }
  }

  private void fixContextFolders(Xpp3Dom configuration, Collection<File> contextFolders) throws IOException {
	    if (contextFolders == null || contextFolders.isEmpty()) {
	    	return;
	    }
	    StringBuilder customContextFolders = new StringBuilder();
	    boolean addComma = false;
	    for(File folder : contextFolders) {
	    	if (addComma) {
	    		customContextFolders.append(", ");
	    	}
	    	customContextFolders.append(folder.getAbsolutePath().replace('\\', '/'));
	    	addComma = true;
	    }
	    Xpp3Dom dom = configuration.getChild(CONTEXT_FOLDER);
	    if (dom == null) {
	       dom = new Xpp3Dom(CONTEXT_FOLDER);
	       configuration.addChild(dom);
	    }
	    dom.setValue(customContextFolders.toString());
	  }
  
  private IPath getReplacementPath(File originalFolder, IFolder webResourcesFolder, IPath defaultOutputPathPrefix)
      throws IOException {
    IPath originalDestinationFolderPath = Path.fromOSString(originalFolder.getCanonicalPath());
    
    if (!defaultOutputPathPrefix.isPrefixOf(originalDestinationFolderPath)) {
      return null;
    }
    
    IPath relativePath = originalDestinationFolderPath.makeRelativeTo(defaultOutputPathPrefix);
    IPath customPath = webResourcesFolder.getLocation().append(relativePath);
    return customPath;
  }

  private boolean notCleanFullBuild(int kind) {
    return IncrementalProjectBuilder.FULL_BUILD != kind
        && IncrementalProjectBuilder.CLEAN_BUILD != kind;
  }
  
  private static class CleanBuildContext implements BuildContext {

	private BuildContext originalContext;

	CleanBuildContext(BuildContext originalContext) {
		this.originalContext = originalContext;
	}
	  
	public boolean hasDelta(String relpath) {
		return true;
	}

	public boolean hasDelta(File file) {
		return true;
	}

	public boolean hasDelta(List relpaths) {
		return true;
	}

	public void refresh(File file) {
		originalContext.refresh(file);
	}

	public OutputStream newFileOutputStream(File file) throws IOException {
		return originalContext.newFileOutputStream(file);
	}

	public Scanner newScanner(File basedir) {
		return originalContext.newScanner(basedir);
	}

	public Scanner newDeleteScanner(File basedir) {
		return originalContext.newDeleteScanner(basedir);
	}

	public Scanner newScanner(File basedir, boolean ignoreDelta) {
		return originalContext.newScanner(basedir, ignoreDelta);
	}

	public boolean isIncremental() {
		return false;
	}

	public void setValue(String key, Object value) {
		originalContext.setValue(key, value);
	}

	public Object getValue(String key) {
		return originalContext.getValue(key);
	}

	public void addWarning(File file, int line, int column, String message,
			Throwable cause) {
		originalContext.addWarning(file, line, column, message, cause);
	}

	public void addError(File file, int line, int column, String message,
			Throwable cause) {
		originalContext.addError(file, line, column, message, cause);
	}

	public void addMessage(File file, int line, int column, String message,
			int severity, Throwable cause) {
		originalContext.addMessage(file, line, column, message, severity, cause);
	}

	public void removeMessages(File file) {
		originalContext.removeMessages(file);
	}

	public boolean isUptodate(File target, File source) {
		return false;
	}

  }

}
