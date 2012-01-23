/*******************************************************************************
 * Copyright (c) 2012 Red Hat, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/

package org.jboss.tools.m2e.wro4j.internal;

import java.io.File;
import java.io.IOException;
import java.util.Set;

import org.apache.maven.plugin.MojoExecution;
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
import org.eclipse.core.runtime.Path;
import org.eclipse.m2e.core.MavenPlugin;
import org.eclipse.m2e.core.embedder.IMaven;
import org.eclipse.m2e.core.project.IMavenProjectFacade;
import org.eclipse.m2e.core.project.MavenProjectUtils;
import org.eclipse.m2e.core.project.configurator.MojoExecutionBuildParticipant;
import org.sonatype.plexus.build.incremental.BuildContext;

public class Wro4jBuildParticipant extends MojoExecutionBuildParticipant {

  private static final String DESTINATION_FOLDER = "destinationFolder";
  private static final String CSS_DESTINATION_FOLDER = "cssDestinationFolder";
  private static final String JS_DESTINATION_FOLDER = "jsDestinationFolder";

  public Wro4jBuildParticipant(MojoExecution execution) {
    super(execution, true);
  }

  @Override
  public Set<IProject> build(int kind, IProgressMonitor monitor)
      throws Exception {

    MojoExecution mojoExecution = getMojoExecution();
    if (mojoExecution == null) {
      return null;
    }

    BuildContext buildContext = getBuildContext();
    if (notCleanFullBuild(kind)
        && !wroResourceChangeDetected(mojoExecution, buildContext)) {
      return null;
    }

    Xpp3Dom originalConfiguration = mojoExecution.getConfiguration();

    Set<IProject> result = null;
    try {

      File destinationFolder = getFolder(mojoExecution, DESTINATION_FOLDER);
      File jsDestinationFolder = getFolder(mojoExecution, JS_DESTINATION_FOLDER);
      File cssDestinationFolder = getFolder(mojoExecution, CSS_DESTINATION_FOLDER);

      Xpp3Dom customConfiguration = customize(originalConfiguration, 
                                              destinationFolder, 
                                              jsDestinationFolder, 
                                              cssDestinationFolder);
      // Add custom configuration
      mojoExecution.setConfiguration(customConfiguration);

      // execute mojo
      result = super.build(kind, monitor);

      // tell m2e builder to refresh generated folder
      refreshFolders(mojoExecution, buildContext);

    } finally {
      // restore original configuration
      mojoExecution.setConfiguration(originalConfiguration);
    }

    return result;
  }

  private File getFolder(MojoExecution mojoExecution, String folderName)
      throws CoreException {
    IMaven maven = MavenPlugin.getMaven();
    File folder = maven.getMojoParameterValue(getSession(), mojoExecution, folderName, File.class);
    return folder;
  }

  private boolean wroResourceChangeDetected(MojoExecution mojoExecution,
      BuildContext buildContext) throws CoreException {

    // If the pom file changed, we force wro4j's invocation
    if (isPomModified()) {
      return true;
    }

    // check if any of the web resource files changed
    File source = getFolder(mojoExecution, "contextFolder");
    // TODO also analyze output classes folders as wro4j can use classpath files
    Scanner ds = buildContext.newScanner(source); // delta or full scanner
    ds.scan();
    String[] includedFiles = ds.getIncludedFiles();
    if (includedFiles == null || includedFiles.length <= 0) {
      return false;
    }

    // Quick'n dirty trick to avoid calling wro4j for ANY file change
    // Let's restrict ourselves to a few known extensions
    for (String file : includedFiles) {
      // FIXME That list is completely arbitrary
      if (file.matches("([^\\s]+(\\.(?i)(js|css|scss|sass|less|coffee|json))$)")) {
        return true;
      }
    }

    // TODO analyze wro.xml for file patterns,
    // check if includedFiles match wro4j-maven-plugin's select targetGroups
    // filePatterns
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

  private void refreshFolders(MojoExecution mojoExecution,
      BuildContext buildContext) throws CoreException {
    refreshFolder(mojoExecution, buildContext, DESTINATION_FOLDER);
    refreshFolder(mojoExecution, buildContext, CSS_DESTINATION_FOLDER);
    refreshFolder(mojoExecution, buildContext, JS_DESTINATION_FOLDER);
  }

  private void refreshFolder(MojoExecution mojoExecution, 
      BuildContext buildContext, String directoryName) throws CoreException {
    File outputDirectory = getFolder(mojoExecution, directoryName);
    if (outputDirectory != null && outputDirectory.exists()) {
      buildContext.refresh(outputDirectory);
    }
  }

  private Xpp3Dom customize(Xpp3Dom originalConfiguration,
      File originalDestinationFolder, File originalJsDestinationFolder,
      File originalCssDestinationFolder) throws IOException {
    IMavenProjectFacade facade = getMavenProjectFacade();
    if (!"war".equals(facade.getPackaging())) {
      // Not a war project, we don't know how to customize that
      return originalConfiguration;
    }

    IProject project = facade.getProject();
    String target = facade.getMavenProject().getBuild().getDirectory();
    IPath relativeTargetPath = MavenProjectUtils.getProjectRelativePath(project, target);
    if (relativeTargetPath == null) {
      // target folder not under the project directory, we bail
      return originalConfiguration;
    }

    IFolder webResourcesFolder = project.getFolder(relativeTargetPath.append("m2e-wtp").append("web-resources"));
    if (!webResourcesFolder.exists()) {
      // Not a m2e-wtp project, we don't know how to customize either
      // TODO Try to support Sonatype's webby instead?
      return originalConfiguration;
    }

    IPath fullTargetPath = new Path(target);
    IPath defaultOutputPathPrefix = fullTargetPath.append(facade.getMavenProject().getBuild().getFinalName());

    Xpp3Dom customConfiguration = new Xpp3Dom("configuration");
    Xpp3DomUtils.mergeXpp3Dom(customConfiguration, originalConfiguration);

    customizeFolder(originalDestinationFolder, webResourcesFolder,
        defaultOutputPathPrefix, customConfiguration, DESTINATION_FOLDER);

    customizeFolder(originalJsDestinationFolder, webResourcesFolder,
        defaultOutputPathPrefix, customConfiguration, JS_DESTINATION_FOLDER);

    customizeFolder(originalCssDestinationFolder, webResourcesFolder,
        defaultOutputPathPrefix, customConfiguration, CSS_DESTINATION_FOLDER);

    return customConfiguration;
  }

  private void customizeFolder(File originalDestinationFolder,
      IFolder webResourcesFolder, IPath defaultOutputPathPrefix,
      Xpp3Dom configuration, String folderParameterName) throws IOException {

    if (originalDestinationFolder != null) {
      IPath customPath = getReplacementPath(originalDestinationFolder, webResourcesFolder, defaultOutputPathPrefix);
      if (customPath != null) {
        Xpp3Dom dom = configuration.getChild(folderParameterName);
        if (dom == null) {
          dom = new Xpp3Dom(folderParameterName);
          configuration.addChild(dom);
        }
        dom.setValue(customPath.toOSString());
      }
    }
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
}
