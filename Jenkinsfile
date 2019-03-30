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
  * Pipeline Utility Steps
  * Warnings 5
  */
 
pipeline { 
    agent {
        dockerfile {
            filename 'Jenkins.Dockerfile'
            args '-v $HOME/.m2:/root/.m2 --network="host"'
        }
    }
    environment {
        JAVA_HOME = '/usr/lib/jvm/java-11-openjdk-amd64'
    }
    stages {
        stage('Build') { 
            steps {
                configFileProvider([configFile(fileId: 'maven-settings', variable: 'MAVEN_SETTINGS')]){ 
                    sh 'mvn -s $MAVEN_SETTINGS -DskipTests=true clean package'
                }
            }
        }
        stage('Check') { 
            steps {
                configFileProvider([configFile(fileId: 'maven-settings', variable: 'MAVEN_SETTINGS')]){  
                    sh 'mvn -s $MAVEN_SETTINGS spotbugs:spotbugs'
                }
            }
        }
        stage('Test') { 
            steps {
               configFileProvider([configFile(fileId: 'maven-settings', variable: 'MAVEN_SETTINGS')]){   
                   sh 'mvn -s $MAVEN_SETTINGS test'
               }
            }
        }
        stage('Deploy') {
            when {
                anyOf{
                    branch 'develop';
                    branch 'master';
                }
            }
            steps {
                configFileProvider([configFile(fileId: 'maven-settings', variable: 'MAVEN_SETTINGS')]){ 
                    sh 'mvn -s $MAVEN_SETTINGS -DskipTests=true deploy'
                }
                withDockerRegistry([ credentialsId: "local-docker-registry-credentials", url: "localhost:8081/repository/badamson/" ]) {
     				script {
     					def VERSION = readMavenPom().getVersion()
                    	def image = docker.build("mc:${VERSION}", "-f ../Dockerfile target")
                    	image.push()
                	}
   				}
            }
        }
    }
    post {
        always {// We ESPECIALLY want the reports on failure
            script {
                def spotbugs = scanForIssues tool: [$class: 'SpotBugs'], pattern: 'target/spotbugsXml.xml'
                publishIssues issues:[spotbugs]
            }
            junit 'target/surefire-reports/**/*.xml'  
        }
        success {
            archiveArtifacts artifacts: 'target/MC-*.jar', fingerprint: true
        }
    }
}