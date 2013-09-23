package org.jboss.tools.m2e.wro4j.tests;

import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.IOUtils;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceDescription;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.m2e.tests.common.AbstractMavenProjectTestCase;

public class Wro4jProjectconfiguratorTest extends AbstractMavenProjectTestCase {

	public void testM2eWtpDestinationFolderSupport() throws Exception {
		IProject p = importProject("projects/p1/pom.xml");
		waitForJobsToComplete();

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

		js = p.getFile("target/m2e-wtp/web-resources/resources/testCase.js");
		assertTrue("testCase.js is missing after a clean build", js.exists());
		css = p.getFile("target/m2e-wtp/web-resources/resources/testCase.css");
		assertTrue("testCase.css is missing after a clean build", css.exists());
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

	protected static String getAsString(IFile file) throws IOException,
			CoreException {
		assert file != null;
		assert file.isAccessible();
		InputStream ins = null;
		String content = null;
		try {
			ins = file.getContents();
			content = IOUtils.toString(ins).replaceAll("\r\n", "\n");
		} finally {
			IOUtils.closeQuietly(ins);
		}
		return content;
	}

}
