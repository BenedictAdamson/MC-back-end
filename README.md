# MC
The Mission Command game.

## License

© Copyright Benedict Adamson 2018-20.
 
![AGPLV3](https://www.gnu.org/graphics/agplv3-with-text-162x68.png)

MC is free software: you can redistribute it and/or modify
it under the terms of the
[GNU Affero General Public License](https://www.gnu.org/licenses/agpl.html)
as published by the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

MC is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU Affero General Public License for more details.

You should have received a copy of the GNU Affero General Public License
along with MC.  If not, see <https://www.gnu.org/licenses/>.

## Components

MC provides a web interface: you play it using a web browser, using a URL that indicates the installation (server) of MC that you wish to play. An installation of MC has several components that together provide the information used by your browser:
* A *front-end*, which runs in the browser.
* An *HTTP reverse proxy* (also call the *ingress*), which communicates with the browser. It forwards requests from the browser (or front-end) to the *front-end HTTP server* and to the *back end HTTP server*, as appropriate for each request.
* A *front-end HTTP server*, which provides your browser with the code for the front-end.
* A *back end HTTP server*, which does the complicated processing.
* A *game database server*, which records long term information for the back end HTTP server.
* An *authentication server*, which handles logins.
* An *authentication database server*, which records long term information authentication server.

The back end HTTP server makes use of code in the *model* component.
An *authentication initialisation job* sets up basic authentication information.

The system has separate front-end and back-end HTTP servers because of the technical difficulty of combining them.
The front-end HTTP serving must provide identical static front-end code at multiple URLs,
because of the [Angular](https://angular.io/) *routing* of the front-end.
The back-end HTTP serving, by [Spring Boot](http://spring.io/projects/spring-boot),
is more suitable for fixed routing of static resources, with each resource having only one URL.

The system has separate game and authentication database servers because the (third party) authentication server does support the kind of database server that is suitable for the game itself.

## Public Repositories

MC is available from these public repositories:
* Source code: [https://github.com/BenedictAdamson/MC](https://github.com/BenedictAdamson/MC)
* Docker images:
    * [https://hub.docker.com/r/benedictadamson/mc-auth](https://hub.docker.com/r/benedictadamson/mc-auth)
    * [https://hub.docker.com/r/benedictadamson/mc-auth-db](https://hub.docker.com/r/benedictadamson/mc-auth-db)
    * [https://hub.docker.com/r/benedictadamson/mc-auth-init](https://hub.docker.com/r/benedictadamson/mc-auth-inint)
    * [https://hub.docker.com/r/benedictadamson/mc-back-end](https://hub.docker.com/r/benedictadamson/mc-back-end)
    * [https://hub.docker.com/r/benedictadamson/mc-database](https://hub.docker.com/r/benedictadamson/mc-database)
    * [https://hub.docker.com/r/benedictadamson/mc-front-end-srv](https://hub.docker.com/r/benedictadamson/mc-front-end-srv)

## Technologies Used

* Languages:
    * [Java 11](https://docs.oracle.com/javase/11/)
    * [Typescript](https://www.typescriptlang.org/)
* Servers:
    * [Keycloak](https://www.keycloak.org/)
    * [MariaDB](https://mariadb.com/)
    * [mongoDB](https://www.mongodb.com/)
    * [NGINX](https://www.nginx.com/)
* Java Annotations:
    * [JCIP annotations](http://jcip.net/annotations/doc/net/jcip/annotations/package-summary.html)
    * [SpotBugs annotations](https://javadoc.io/doc/com.github.spotbugs/spotbugs-annotations/3.1.8)
* Development environment:
    * [Eclipse IDE](https://www.eclipse.org/ide/)
    * [Cucumber Eclipse Plugin](https://marketplace.eclipse.org/content/cucumber-eclipse-plugin)
    * [Eclipse Docker Tooling](https://marketplace.eclipse.org/content/eclipse-docker-tooling)
    * [Eclipse Jenkins Editor](https://github.com/de-jcup/eclipse-jenkins-editor)
    * [Eclipse-PMD plugin](https://marketplace.eclipse.org/content/eclipse-pmd)
    * [Eclipse SpotBugs Plugin](https://marketplace.eclipse.org/content/spotbugs-eclipse-plugin)
    * [Eclipse Wild Web Developer](https://projects.eclipse.org/proposals/eclipse-wild-web-developer)
    * [Minikube](https://github.com/kubernetes/minikube)
* Software configuration management:
     * [Chartmuseum](https://github.com/helm/chartmuseum)
     * [Git](https://git-scm.com/)
     * [GitHub](https://github.com)
     * [Sonatype Nexus Repository Manager OSS](https://www.sonatype.com/nexus-repository-oss)
* Building:
    * [Docker](https://www.docker.com/)
    * [Jenkins](https://jenkins.io/)
    * [Maven](https://maven.apache.org/), including various Maven plugins provided by the Maven project
        * [Helm Maven Plugin](https://github.com/kiwigrid/helm-maven-plugin)
        * [Maven Frontend Plugin](https://github.com/eirslett/frontend-maven-plugin)
        * [Spring Boot Maven Plugin](https://docs.spring.io/spring-boot/docs/2.1.3.RELEASE/maven-plugin/)
        * [SpotBugs Maven Plugin](https://spotbugs.github.io/spotbugs-maven-plugin/index.html)
        * Spotify [Dockerfile Maven Plugin](https://github.com/spotify/dockerfile-maven)
    * [Ubuntu 18.04](http://releases.ubuntu.com/18.04/)
    * [Debian Backports](https://backports.debian.org/)
* Static analysis and testing:
    * [Cucumber](https://docs.cucumber.io/cucumber/):
        * [Cucumber JVM](https://docs.cucumber.io/installation/java/)
        * [Cucumber-Spring](https://docs.cucumber.io/cucumber/state/#spring)
    * [Jasmine](https://jasmine.github.io/index.html)
    * [JUnit 5](https://junit.org/junit5/)
    * [Java Hamcrest](http://hamcrest.org/JavaHamcrest/)
    * [Karma](https://karma-runner.github.io/latest/index.html)
    * [Open Test Alliance for the JVM](https://github.com/ota4j-team/opentest4j)
    * [PMD](https://pmd.github.io/)
    * [SpotBugs](https://spotbugs.github.io/)
* Frameworks:
    * [Angular](https://angular.io/)
    * [Spring Boot](http://spring.io/projects/spring-boot)
    * [Spring Framework](https://spring.io/projects/spring-framework)
        * [Spring Security](https://spring.io/projects/spring-security)
        * [Spring WebFlux](https://docs.spring.io/spring/docs/current/spring-framework-reference/web-reactive.html)
* Deployment:
    * [Docker Hub](https://hub.docker.com/)
    * [Helm](https://helm.sh/)
    * [Kubernetes](https://kubernetes.io/)
 