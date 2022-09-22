# MC
The Mission Command game.

## License

© Copyright Benedict Adamson 2018-22.
 
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
* A *front-end HTTP server*, which provides your browser with the code for the front-end.
* A *back end HTTP server*, which does the complicated processing.
* A *database server*, which records long term information for the back end HTTP server.
* Optionally, an *HTTP reverse proxy* (also call the *ingress*), which communicates with the browser. It forwards requests from the browser (or front-end) to the *front-end HTTP server* and to the *back end HTTP server*, as appropriate for each request.

The back end HTTP server makes use of code in the *model* component.
The servers are processes, and so could run on one computer, or several.

The system has separate front-end and back-end HTTP servers because of the technical difficulty of combining them.
The front-end HTTP serving must provide identical static front-end code at multiple URLs,
because of the [Angular](https://angular.io/) *routing* of the front-end.
The back-end HTTP serving, by [Spring Boot](http://spring.io/projects/spring-boot),
is more suitable for fixed routing of resources, with each resource having only one URL.

The build system makes the software available as a collection of Debian installation packages
(also suitable for Ubuntu, and other Debian derivatives),
and as a Helm chart and Docker images.
When using the Debian packages, the *database server* component must be installed on a computer
that has a MongoDB server installed,
which can be done by following the
[MongoDB installation instructions](https://docs.mongodb.com/manual/tutorial/install-mongodb-on-debian/).

## Public Repositories

MC is available from these public repositories:
* Source code: [https://github.com/BenedictAdamson/MC](https://github.com/BenedictAdamson/MC)
* Docker images:
    * [https://hub.docker.com/r/benedictadamson/mc-back-end](https://hub.docker.com/r/benedictadamson/mc-back-end)
    * [https://hub.docker.com/r/benedictadamson/mc-database](https://hub.docker.com/r/benedictadamson/mc-database)
    * [https://hub.docker.com/r/benedictadamson/mc-front-end-srv](https://hub.docker.com/r/benedictadamson/mc-front-end-srv)
