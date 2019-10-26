# Dockerfile for the use in the Jenkinsfile for the MC project,
# to set up the build environment for Jenkins to use.

# © Copyright Benedict Adamson 2018-19.
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

# Need Docker, Java 11 and Maven.
# Also need nodejs, npm and Angular,
# but the frontend-maven-plugin installs those.

FROM debian:10
RUN apt-get -y update && apt-get -y install \
   apt-transport-https \
   ca-certificates \
   curl \
   firefox-esr \
   gnupg-agent \
   golang-go \
   maven \
   openjdk-11-jdk-headless \
   software-properties-common
RUN curl -fsSL https://download.docker.com/linux/debian/gpg | apt-key add -
RUN add-apt-repository -y \
   "deb [arch=amd64] https://download.docker.com/linux/debian $(lsb_release -cs) stable"
RUN apt-get -y update && apt-get -y install \
   containerd.io \
   docker-ce \
   docker-ce-cli
RUN cd /tmp && \
   curl -o /usr/bin/kubectl https://storage.googleapis.com/kubernetes-release/release/v1.16.2/bin/linux/amd64/kubectl
RUN cd /tmp && \
   curl -o helm.tar.gz https://get.helm.sh/helm-v2.15.1-linux-amd64.tar.gz && \
   tar -xf helm.tar.gz linux-amd64/helm && \
   mv linux-amd64/helm /usr/bin
   