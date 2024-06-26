# Dockerfile for the use by Jenkins for the MC project,
# to set up the build environment for Jenkins to use.

# © Copyright Benedict Adamson 2018-24.
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

# Need Chrome, Docker, Helm, Java 11 and Maven.
# Also need nodejs, npm and Angular,
# but the frontend-maven-plugin installs those.

FROM debian:11

ARG JENKINSUID
ARG JENKINSGID
ARG DOCKERGID

RUN apt-get -y update && apt-get -y install \
   apt-transport-https \
   bzip2 \
   ca-certificates \
   curl \
   gnupg-agent \
   maven \
   openjdk-17-jdk-headless \
   software-properties-common
# Add third-party repositories
RUN apt-get remove -y openjdk-11-jre-headless
# Add Chrome repository
RUN curl -fsSL https://dl-ssl.google.com/linux/linux_signing_key.pub | apt-key add -
RUN add-apt-repository -y \
   "deb [arch=amd64] http://dl.google.com/linux/chrome/deb/ stable main"
# Add Docker repository
RUN curl -fsSL https://download.docker.com/linux/debian/gpg | apt-key add -
RUN add-apt-repository -y \
   "deb [arch=amd64] https://download.docker.com/linux/debian $(lsb_release -cs) stable"
# Add Helm repository
RUN curl https://helm.baltorepo.com/organization/signing.asc | apt-key add -
RUN echo "deb https://baltocdn.com/helm/stable/debian/ all main" > /etc/apt/sources.list.d/helm-stable-debian.list
# Add aptly repository
RUN echo "deb http://repo.aptly.info/ squeeze main" > /etc/apt/sources.list.d/aptly.list
RUN apt-key adv --keyserver keyserver.ubuntu.com --recv-keys A0546A43624A8331
# Install third-party packages
RUN apt-get -y update && apt-get -y install \
   aptly \
   containerd.io \
   docker-ce \
   docker-ce-cli \
   google-chrome-stable \
   helm

# Setup users and groups
RUN groupadd -g ${JENKINSGID} jenkins
RUN groupmod -g ${DOCKERGID} docker
RUN useradd -c "Jenkins user" -g ${JENKINSGID} -G ${DOCKERGID} -M -N -u ${JENKINSUID} jenkins

WORKDIR /home/jenkins/agent