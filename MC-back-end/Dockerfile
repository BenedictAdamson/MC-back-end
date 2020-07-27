# Dockerfile for the MC-back-end project,
# to produce the MC server in a container.

# © Copyright Benedict Adamson 2018-20.
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
# along with MC-des.  If not, see <https://www.gnu.org/licenses/>.
#

FROM openjdk:11
ARG VERSION
LABEL version="${VERSION}"
LABEL description="The Mission Command game back-end server."
LABEL maintainer="badamson@spamcop.net"
EXPOSE 8080
RUN groupadd -g 1024 mc
RUN useradd -c "Mission Command server" --create-home -u 1024 -g 1024 mc
USER mc
WORKDIR /home/mc
COPY target/MC-back-end-${VERSION}.jar MC-back-end.jar
ENTRYPOINT ["java","-jar","MC-back-end.jar"]
CMD []