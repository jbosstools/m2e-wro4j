package org.jboss.tools.m2e.wro4j.tests;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.m2e.tests.common.AbstractMavenProjectTestCase;

public class Wro4jProjectconfiguratorTest extends AbstractMavenProjectTestCase {

  public void testM2eWtpDestinationFolderSupport() throws Exception {
    IProject p = importProject("projects/p1/pom.xml");
    waitForJobsToComplete();

    p.build(IncrementalProjectBuilder.AUTO_BUILD, monitor);
    waitForJobsToComplete();

    IFile js = p.getFile("target/m2e-wtp/web-resources/resources/testCase.js");
    assertTrue("testCase.js is missing", js.exists());
    IFile css = p.getFile("target/m2e-wtp/web-resources/resources/testCase.css");
    assertTrue("testCase.css is missing", css.exists());
  }

  public void testM2eWtpGroupNameMappingFileSupport() throws Exception {
    IProject p = importProject("projects/p2/pom.xml");
    waitForJobsToComplete();

    p.build(IncrementalProjectBuilder.AUTO_BUILD, monitor);
    waitForJobsToComplete();

    IFile css = p.getFile("target/m2e-wtp/web-resources/resources/testCase.css");
    assertTrue("testCase.css is missing", css.exists());
    IFile mapping = p.getFile("target/m2e-wtp/web-resources/resources/mapping.txt");
    assertTrue("mapping.txt is missing", mapping.exists());
  }

}
