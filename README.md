M2E connector for WRO4J
========================

This m2e connector for [WRO4J](https://code.google.com/p/wro4j/) will execute wro4j-maven-plugin:run on Eclipse incremental builds,
if a change is detected on css, js, less, json, sass resources under wro4j-maven-plugin's contextFolder (src/main/webapp by default)

In order for m2e-wro4j to be enabled, projects in eclipse must be Maven-enabled with m2e (right-click on project > Configure > Convert to Maven...)

If m2e-wtp is installed and wro4j's target directories are set under ${project.build.directory}/${project.build.finalName/ then the resources 
will be generated under ${project.build.directory}/m2e-wtp/web-resources/ so they can be picked up and deployed by WTP on the fly.

**m2e-wro4j 1.2.x** requires at least m2e 2.x and m2e-wtp 1.5.x.

If the Eclipse Marketplace Client is installed in you Eclipse distribution, you can install the latest m2e-wro4j version by dragging and dropping the following image in your Eclipse workbench :

[![Install m2e-wro4j](https://marketplace.eclipse.org/sites/all/modules/custom/marketplace/images/installbutton.png "Drag and drop into a running Eclipse Indigo workspace to install m2e-wro4j")](http://marketplace.eclipse.org/marketplace-client-intro?mpc_install=807489)


Alternatively, you can install m2e-wro4j from regular P2 update sites : 

* New releases are available from http://download.jboss.org/jbosstools/updates/m2e-wro4j/ 
* Dev builds can be installed from https://github.com/jbosstools/m2e-wro4j/releases/download/latest/ 
