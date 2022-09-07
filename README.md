M2E connector for WRO4J
========================

This m2e connector for [WRO4J](https://github.com/wro4j/wro4j) will execute wro4j-maven-plugin:run on Eclipse incremental builds,
if a change is detected on css, js, less, json, sass resources under wro4j-maven-plugin's contextFolder (src/main/webapp by default)

In order for m2e-wro4j to be enabled, projects in eclipse must be Maven-enabled with m2e (right-click on project > Configure > Convert to Maven...)

If m2e-wtp is installed and wro4j's target directories are set under ${project.build.directory}/${project.build.finalName/ then the resources 
will be generated under ${project.build.directory}/m2e-wtp/web-resources/ so they can be picked up and deployed by WTP on the fly.

Requirements
------------
m2e-wro4j 1.1.x requires a minimum of:
- Java 7
- Eclipse 2022-09
- m2e 1.5.x - 1.20.x 
- m2e-wtp <= 1.4.x 

**m2e-wro4j 1.2.x** requires a minimum of:
- Java 17
- Eclipse 2022-09
- m2e 2.x 
- m2e-wtp 1.5.x 

Installation
------------

_M2E connector for WRO4J_ is available in the [Eclipse Marketplace](https://marketplace.eclipse.org/content/m2e-wro4j). Drag the following button to your running Eclipse workspace. (⚠️ *Requires the Eclipse Marketplace Client*)

[![Drag to your running Eclipse* workspace. *Requires Eclipse Marketplace Client](https://marketplace.eclipse.org/sites/all/themes/solstice/public/images/marketplace/btn-install.svg)](http://marketplace.eclipse.org/marketplace-client-intro?mpc_install=807489 "Drag to your running Eclipse* workspace. *Requires Eclipse Marketplace Client")

Alternatively, in Eclipse:

- open Help > Install New Software...
- work with: 
    * `https://github.com/jbosstools/m2e-wro4j/releases/download/latest/` for CI builds
    * `http://download.jboss.org/jbosstools/updates/m2e-wro4j/` for released builds
- expand the category and select the `m2e connector for WRO4J`
- proceed with the installation
- restart Eclipse


Build
-----

Open a terminal and execute:

    ./mvnw clean package
    
You can then install the generated update site from `org.jboss.tools.m2e.wro4j.site/target/site`, also zipped as `org.jboss.tools.m2e.wro4j.site-<VERSION>-SNAPSHOT-site.zip`

License
-------
EPL 2.0, See [LICENSE](LICENSE) file.