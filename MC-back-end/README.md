# MC-back-end
The back end HTTPD server of the Mission Command game.

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

## Overview

This component does the complicated processing for an installation of MC.
It communicates with a *database server*, which records long term information.
It makes use of code in the *model* component.

This component is a [Spring Boot](http://spring.io/projects/spring-boot) application,
built using [Maven](https://maven.apache.org/)
and the [Spring Boot Maven Plugin](https://docs.spring.io/spring-boot/docs/2.1.3.RELEASE/maven-plugin/).
That results in an executable JAR.
The component is also built as a [Docker](https://www.docker.com/) image,
which is published on Docker Hub at
[https://hub.docker.com/r/benedictadamson/mc-back-end](https://hub.docker.com/r/benedictadamson/mc-back-end).

Because a Spring Boot executable JAR can not also be used as a library JAR,
code that might be shared with other Java components of MC
(and in particular, is used by integration test code)
is in the *model* component rather than this component.

## Configuration

The back end HTTPD server of Mission Command is a Spring Boot application,
and so can be configured by setting Spring properties
(using environment variables, Java properties, YAML properties and command-line arguments)
like other Spring Boot applications.
* It uses the MongoDB drivers, so the `spring.data.mongodb.*` properties are available.
  In particular, `spring.data.mongodb.host` and `spring.data.mongodb.password`
  should be set to enable connection to the database server.
* The `administrator.password` property is the (unencrypted) password of the special administrator user of the system.