package org.jboss.tools.m2e.wro4j.tests;

import org.apache.commons.io.IOUtils;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
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

    IFile css = p.getFile("target/m2e-wtp/web-resources/resources/testCase.css");
    assertTrue("testCase.css is missing", css.exists());
    IFile mapping = p.getFile("target/m2e-wtp/web-resources/resources/mapping.txt");
    assertTrue("mapping.txt is missing", mapping.exists());
  }


  public void testRebuildOnConfigChange() throws Exception {
    IProject p = importProject("projects/p2/pom.xml");
    waitForJobsToComplete();

    p.build(IncrementalProjectBuilder.AUTO_BUILD, monitor);
    waitForJobsToComplete();

    IFile css = p.getFile("target/m2e-wtp/web-resources/resources/testCase.css");
    assertTrue("testCase.css is missing", css.exists());
    
    IFile wroXml = p.getFile("src/main/webapp/WEB-INF/wro.xml");
    
    String wro = "<groups xmlns=\"http://www.isdc.ro/wro\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\"\r\n" + 
    		"	xsi:schemaLocation=\"http://www.isdc.ro/wro wro.xsd\">\r\n" + 
    		"	<group name=\"newStyle\">\r\n" + 
    		"		<css>/*.css</css>\r\n" + 
    		"	</group>\r\n" + 
    		"</groups>";
	wroXml.setContents(IOUtils.toInputStream(wro ), IResource.FORCE, monitor);


    p.build(IncrementalProjectBuilder.AUTO_BUILD, monitor);
    waitForJobsToComplete();

    IFile newStyle = p.getFile("target/m2e-wtp/web-resources/resources/newStyle.css");
    assertTrue("newStyle.css is missing", newStyle.exists());

    
  }
  
  
}
