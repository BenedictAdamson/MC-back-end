# MC-back-end
The back-end HTTP server of the Mission Command game.

## License

© Copyright Benedict Adamson 2018-23.
 
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

## Description

This project provides the *back-end HTTP server* of the system, which does the complicated processing.
It communicates with a *database server*, which records long term information for the back-end HTTP server.
When installed it would be accessed through an *HTTP reverse proxy* (also call the *ingress*),
which communicates with the browser.

The back-end HTTP server makes use of code in the *model* component.
The back-end HTTP server could run on one computer, or several.

The system has separate front-end and back-end HTTP servers because of the technical difficulty of combining them.
The front-end HTTP serving must provide identical static front-end code at multiple URLs,
because of the [Angular](https://angular.io/) *routing* of the front-end.
The back-end HTTP serving, by [Spring Boot](http://spring.io/projects/spring-boot),
is more suitable for fixed routing of resources, with each resource having only one URL.

The build system makes the server available as a Debian installation package
(also suitable for Ubuntu, and other Debian derivatives),
and as a Docker images.

## Public Repositories

The MC back-end is available from these public repositories:
* Source code: [https://github.com/BenedictAdamson/MC](https://github.com/BenedictAdamson/MC-back-end)
* Docker image: [https://hub.docker.com/r/benedictadamson/mc-spring](https://hub.docker.com/r/benedictadamson/mc-spring)
