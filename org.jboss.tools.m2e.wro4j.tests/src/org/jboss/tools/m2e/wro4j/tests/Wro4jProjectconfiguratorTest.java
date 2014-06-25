/*******************************************************************************
 * Copyright (c) 2012-2013 Red Hat, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *******************************************************************************/
package org.jboss.tools.m2e.wro4j.tests;

import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.IOUtils;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceDescription;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.m2e.core.project.ResolverConfiguration;
import org.eclipse.m2e.tests.common.AbstractMavenProjectTestCase;

public class Wro4jProjectconfiguratorTest extends AbstractMavenProjectTestCase {

	public void testM2eWtpDestinationFolderSupport() throws Exception {
		IProject p = importProject("projects/p1/pom.xml");
		waitForJobsToComplete();
		basicTest(p);
	}

	public void testM2eWtpGroupNameMappingFileSupport() throws Exception {
		IProject p = importProject("projects/p2/pom.xml");
		waitForJobsToComplete();

		p.build(IncrementalProjectBuilder.AUTO_BUILD, monitor);
		waitForJobsToComplete();

		IFile css = p
				.getFile("target/m2e-wtp/web-resources/resources/testCase.css");
		assertTrue("testCase.css is missing", css.exists());
		IFile mapping = p
				.getFile("target/m2e-wtp/web-resources/resources/mapping.txt");
		assertTrue("mapping.txt is missing", mapping.exists());
	}

	public void testRebuildOnConfigChange() throws Exception {
		IWorkspaceDescription description = workspace.getDescription();
	    description.setAutoBuilding(true);
	    workspace.setDescription(description);
		
		IProject p = importProject("projects/p3/pom.xml");
		waitForJobsToComplete();
		p.build(IncrementalProjectBuilder.AUTO_BUILD, monitor);
		waitForJobsToComplete();
		
		IFile js = p.getFile("target/m2e-wtp/web-resources/resources/testCase.js");
		assertTrue("testCase.js is missing", js.exists());

		String jsContent = getAsString(js);

		String minifiedJs = "function hello(name){alert(\"Hello \"+name);};";

		assertFalse("javascript should not be minified : \n"+jsContent,jsContent.contains(minifiedJs));

		copyContent(p, "pom-minification.xml", "pom.xml");

		js = p.getFile("target/m2e-wtp/web-resources/resources/testCase.js");
		jsContent = getAsString(js);

		assertTrue("javascript should be minified : \n"+jsContent, jsContent.contains(minifiedJs));

	}

	public void testContextFolder172() throws Exception {
		IProject p = importProject("projects/p4/pom.xml");
		waitForJobsToComplete();
		basicTest(p);
	}	

	public void testContextFolder171() throws Exception {
		IProject p = importProject("projects/p4-171/pom.xml");
		waitForJobsToComplete();
		basicTest(p);
	}	

	public void testContextFolder176() throws Exception {
		IProject p = importProject("projects/p4-176/pom.xml");
		waitForJobsToComplete();
		basicTest(p);
	}	

	public void testMultipleContextFolders() throws Exception {
		IProject p = importProject("projects/p5/pom.xml");
		waitForJobsToComplete();
		basicTest(p);
	}

	public void testMultipleRelativeContextFolders() throws Exception {
		IProject[] projects = importProjects("projects/parent-p6", new String[]{"pom.xml", "p6/pom.xml"}, new ResolverConfiguration());
		waitForJobsToComplete();
		basicTest(projects[1]);
	}

	public void testDisableM2eWtpIntegration() throws Exception {
		IProject p = importProject("projects/p7/pom.xml");
		waitForJobsToComplete();

		p.build(IncrementalProjectBuilder.AUTO_BUILD, monitor);
		waitForJobsToComplete();

		IFile js = p
				.getFile("target/m2e-wtp/web-resources/resources/testCase.js");
		assertFalse(js + " should be missing", js.exists());
		
		js = p
				.getFile("target/disable-wtp-0.0.1-SNAPSHOT/resources/testCase.js");
		assertTrue(js + " is missing", js.exists());
		
		
		IFile css = p
				.getFile("target/m2e-wtp/web-resources/resources/testCase.css");
		assertFalse("target/m2e-wtp/web-resources/resources/testCase.css should be missing", css.exists());

		css = p
				.getFile("target/disable-wtp-0.0.1-SNAPSHOT/resources/testCase.css");
		assertTrue(css + " is missing", css.exists());
    }

	private void assertMinifiedFiles(IProject p) throws Exception {
		IFile js = p.getFile("target/m2e-wtp/web-resources/resources/testCase.js");
		assertTrue("testCase.js is missing after a clean build", js.exists());
		String jsContent = getAsString(js);

		String snippet1 = "function hello(name){alert(\"Hello \"+name);};";
		String snippet2 = "function hi(name){alert(\"Hi \"+name);};";

		assertTrue("javascript should be minified : \n"+jsContent, jsContent.contains(snippet1) && jsContent.contains(snippet2));
		
		IFile css = p
				.getFile("target/m2e-wtp/web-resources/resources/testCase.css");
		assertTrue("testCase.css is missing after a clean build", css.exists());
		String cssContent = getAsString(css);
		snippet1 = "body{background-color:#656565;}";
		snippet2 = ".yeaahbaby{display:hidden;}";

		assertTrue("css should be minified : \n"+cssContent, cssContent.contains(snippet1) && cssContent.contains(snippet2));
	}
	
	
	private void basicTest(IProject p) throws Exception {

		p.build(IncrementalProjectBuilder.AUTO_BUILD, monitor);
		waitForJobsToComplete();

		IFile js = p
				.getFile("target/m2e-wtp/web-resources/resources/testCase.js");
		assertTrue("testCase.js is missing", js.exists());
		IFile css = p
				.getFile("target/m2e-wtp/web-resources/resources/testCase.css");
		assertTrue("testCase.css is missing", css.exists());

		p.build(IncrementalProjectBuilder.CLEAN_BUILD, monitor);
		p.build(IncrementalProjectBuilder.FULL_BUILD, monitor);
		waitForJobsToComplete();

		assertMinifiedFiles(p);
	}	
	
	
	protected static String getAsString(IFile file) throws IOException,
			CoreException {
		assert file != null;
		assert file.isAccessible();
		InputStream ins = null;
		String content = null;
		try {
			file.refreshLocal(IResource.DEPTH_ZERO, new NullProgressMonitor());
			ins = file.getContents();
			content = IOUtils.toString(ins).replaceAll("\r\n", "\n");
		} finally {
			IOUtils.closeQuietly(ins);
		}
		return content;
	}

}
