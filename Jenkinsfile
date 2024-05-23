// Jenkinsfile for the MC-back-end project

/* 
 * © Copyright Benedict Adamson 2018-23.
 * 
 * This file is part of MC.
 *
 * MC is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MC is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with MC.  If not, see <https://www.gnu.org/licenses/>.
 */
 
 /*
  * Jenkins plugins used:
  * Credentials
  * Docker Pipeline
  * Pipeline Utility Steps
  * Warnings Next Generation
  */
 
pipeline { 
    agent {
        dockerfile {
            filename 'Jenkins.Dockerfile'
            additionalBuildArgs  '--build-arg JENKINSUID=`id -u jenkins` --build-arg JENKINSGID=`id -g jenkins` --build-arg DOCKERGID=`stat -c %g /var/run/docker.sock`'
            args '-v $HOME:/home/jenkins -v /var/run/docker.sock:/var/run/docker.sock --network="host" -u jenkins:jenkins --group-add docker'
        }
    }
    triggers {
        pollSCM('H */4 * * *')
    }
    environment {
        JAVA_HOME = '/usr/lib/jvm/java-1.17.0-openjdk-amd64'
        PATH = '/usr/sbin:/usr/bin:/sbin:/bin'
    }
    stages {
        stage('Clean') {
            steps {
                sh './gradlew clean'
            }
        }
        stage('Check, test and publish') {
        	/* Does not push the Docker image. Pushing images of development ("SNAPSHOT") versions can be
        	 * troublesome because it can result in situations when the remote and local repositories hold different
        	 * versions with the same tag, leading to confusion about which version is actually used,
        	 * and inconsistencies for environments (such as minikube and Kubernetes in general) that do not use the
        	 * local Docker registry.
        	 */
            steps {
                withCredentials([usernamePassword(credentialsId: 'maven', usernameVariable: 'USERNAME', passwordVariable: 'PASSWORD')]) {
                    sh './gradlew check test publish buildDockerImage -PmavenUsername=$USERNAME -PmavenPassword=$PASSWORD'
                }
            }
        }
        stage('Push Docker image') {
            when{
                 branch 'main'
            } 
            steps {
                withCredentials([usernamePassword(credentialsId: 'dockerhub', usernameVariable: 'USERNAME', passwordVariable: 'PASSWORD')]) {
                    sh './gradlew pushDockerImage -PdockerhubUsername=$USERNAME -PdockerhubPassword=$PASSWORD'
                }
            }
        }
    }
    post {
        always {// We ESPECIALLY want the reports on failure
            script {
                recordIssues tools: [
                	java(),
                	javaDoc(),
                	mavenConsole(),
                	pmdParser(pattern: '**/build/reports/pmd/*.xml')
					]
            }
            junit 'MC-*/build/test-results/test/TEST-*.xml'
            junit 'MC-*/build/test-results/integrationTest/TEST-*.xml'
        }
        success {
            archiveArtifacts artifacts: 'MC-Spring/build/distributions/*.deb', fingerprint: true
            archiveArtifacts artifacts: 'MC-REST/build/libs/*.jar', fingerprint: true
            archiveArtifacts artifacts: 'MC-Spring/build/libs/*.jar', fingerprint: true
        }
    }
}