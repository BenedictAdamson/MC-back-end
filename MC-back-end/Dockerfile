# Dockerfile for the MC project,
# to produce the MC back-end server in a container.

# © Copyright Benedict Adamson 2018-22.
# 
# This file is part of MC.
#
# MC is free software: you can redistribute it and/or modify
# it under the terms of the GNU Affero General Public License as published by
# the Free Software Foundation, either version 3 of the License, or
# (at your option) any later version.
#
# MC is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
# GNU General Public License for more details.
#
# You should have received a copy of the GNU Affero General Public License
# along with MC.  If not, see <https://www.gnu.org/licenses/>.
#

FROM openjdk:11
ARG VERSION
ARG PACKAGEVERSION
LABEL version="${VERSION}"
LABEL description="The Mission Command game back-end server."
LABEL maintainer="badamson@spamcop.net"
EXPOSE 80
COPY target/missioncommand-be_${PACKAGEVERSION}_all.deb missioncommand-be.deb
# Although the openjdk:11 base image provides a Java 11 JRE, it does not do so by installing a .deb,
# so dpkg will complain of a missing dependency unless we tell it to --force-depends
RUN dpkg -i --force-depends missioncommand-be.deb
# The Debian package installs files that are not useful within a Docker container; remove them to save space:
RUN rm -rf /etc/mission-command /usr/lib/systemd
RUN rm missioncommand-be.deb
USER mission-command
WORKDIR /home/mission-command
ENTRYPOINT ["/usr/bin/mcbe"]
CMD []