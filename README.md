M2E connector for WRO4J
========================

[![Build Status](https://buildhive.cloudbees.com/job/jbosstools/job/m2e-wro4j/badge/icon)](https://buildhive.cloudbees.com/job/jbosstools/job/m2e-wro4j/)

This m2e connector for WRO4J will execute wro4j-maven-plugin:run on Eclipse incremental builds,
if a change is detected on css, js, less, json, sass resources under wro4j-maven-plugin's contextFolder (src/main/webapp by default)

In order for m2e-wro4j to be enabled, projects in eclipse must be Maven-enabled with m2e (right-click on project > Configure > Convert to Maven...)

If m2e-wtp is installed and wro4j's target directories are set under ${project.build.directory}/${project.build.finalName/ then the resources 
will be generated under ${project.build.directory}/m2e-wtp/web-resources/ so they can be picked up and deployed by WTP on the fly.

New releases are available from http://download.jboss.org/jbosstools/updates/m2e-wro4j/

Dev builds can be installed from http://download.jboss.org/jbosstools/builds/staging/m2e-wro4j/all/repo/
