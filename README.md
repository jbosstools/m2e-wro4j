M2E connector for WRO4J
========================

This m2e connector for WRO4J will execute wro4j-maven-plugin:run on Eclipse incremental builds,
if a change is detected on css, js, less, json, sass resources under wro4j-maven-plugin's contextFolder (src/main/webapp by default)


If m2e-wtp is installed and wro4j's target directories are set under ${project.build.directory}/${project.build.finalName/ then the resources 
will be generated under ${project.build.directory}/m2e-wtp/web-resources/ so they can be picked up and deployed by WTP on the fly.


New releases are available from http://download.jboss.org/jbosstools/updates/m2e-wro4j/

Dev builds can be installed from http://download.jboss.org/jbosstools/builds/staging/m2e-wro4j/all/repo/