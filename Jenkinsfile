// Jenkinsfile for the MC project

/* 
 * © Copyright Benedict Adamson 2018-19.
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
  * Config File Provider
  *     - Should configure the file settings.xml with ID 'maven-settings' as the Maven settings file
  *     - That settings.xml configuration should provide authentication credentials
  *       (in server/servers elements) for the services with the following IDs:
  *         - MC.repo: the Maven release repository, at localhost:8081 
  *         - MC-SNAPSHOT.repo: the Maven SNAPSHOT repository, at localhost:8081 
  * Pipeline Utility Steps
  * Warnings 5
  *
  * An administrator will need to permit scripts to use method org.apache.maven.model.Model getVersion.
  */
 
pipeline { 
    agent {
        dockerfile {
            filename 'Jenkins.Dockerfile'
            args '-v $HOME/.m2:/root/.m2 -v /var/run/docker.sock:/var/run/docker.sock --network="host"'
        }
    }
    triggers {
        pollSCM('H */4 * * *')
    }
    environment {
        JAVA_HOME = '/usr/lib/jvm/java-11-openjdk-amd64'
    }
    stages {
        stage('Clean') { 
            steps {
                configFileProvider([configFile(fileId: 'maven-settings', variable: 'MAVEN_SETTINGS')]){ 
                    sh 'mvn -s $MAVEN_SETTINGS clean'
                }
            }
        }
        stage('Build and verify') {
        	/* Includes building Docker images. */ 
            steps {
                configFileProvider([configFile(fileId: 'maven-settings', variable: 'MAVEN_SETTINGS')]){ 
                    sh 'mvn -s $MAVEN_SETTINGS package install test verify spotbugs:check'
                }
            }
        }
        stage('Deploy') {
        	/* Includes pushing Docker images. */ 
            steps {
                configFileProvider([configFile(fileId: 'maven-settings', variable: 'MAVEN_SETTINGS')]){ 
                    sh 'mvn -s $MAVEN_SETTINGS -DskipTests=true deploy'
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
                	pmdParser(pattern: '**/target/pmd.xml'),
					spotBugs(pattern: '**/target/spotbugsXml.xml')
					]
            }
            junit 'MC-back-end/target/*-reports/**/TEST-*.xml'
            junit 'MC-front-end/target/karma-reports/*.xml'  
        }
        success {
            archiveArtifacts artifacts: 'MC-back-end/target/MC-back-end-*.jar', fingerprint: true
            archiveArtifacts artifacts: 'MC-front-end/target/MC-front-end-*.tgz', fingerprint: true
        }
    }
}