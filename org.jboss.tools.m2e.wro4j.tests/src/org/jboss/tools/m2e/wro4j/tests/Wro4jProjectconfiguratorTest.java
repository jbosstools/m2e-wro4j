package org.jboss.tools.m2e.wro4j.tests;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.m2e.tests.common.AbstractMavenProjectTestCase;

public class Wro4jProjectconfiguratorTest extends AbstractMavenProjectTestCase {

  public void testM2eWtpSupport() throws Exception {
    IProject p = importProject("projects/p1/pom.xml");
    waitForJobsToComplete();
    p.build(IncrementalProjectBuilder.AUTO_BUILD, monitor);
    waitForJobsToComplete();
    IFile js = p.getFile("target/m2e-wtp/web-resources/resources/testCase.js");
    assertTrue("testCase.js is missing", js.exists());
    IFile css = p.getFile("target/m2e-wtp/web-resources/resources/testCase.css");
    assertTrue("testCase.css is missing", css.exists());
  }
}
