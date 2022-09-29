def gitCredentialsId = 'github-qoadmin'
def mavenCredentialsId = 'maven-nexus-settings'

pipeline {
	agent any
	environment {
		NEXUS_IP = sh(script: "getent hosts nexus.qodev.es | cut -f1 -d ' ' ", , returnStdout: true).trim()
		REPO_DIR_NAME = "${LIB_NAME}"
		MAVEN_OPTS = '-Duser.home=../.cache'
	}
	parameters {
		string(name: 'BUILD_BRANCH', defaultValue: 'dev', description: 'Rama a construir')
        choice(name: 'LIB_TESTS', choices: ['only_unit', 'unit_and_sonar', 'unit_and_sonar_strict_tests', 'no_tests'], description: 'Tests a ejecutar')

	}
	stages {
		stage('Clean WS') {
			steps {
				script {
				    sh 'echo ${REPO_DIR_NAME}'
					sh '[ $(ls -1 *.tgz 2>/dev/null | wc -l) -gt 0 ] && rm -f *.tgz || exit 0'
					sh '[ $(ls -1 *.jar 2>/dev/null | wc -l) -gt 0 ] && rm -f *.jar || exit 0'
					sh '[ ! -d .cache ] && mkdir .cache || echo .cache exists'
				}
			}
		}
		stage('Clone repository') {

			steps {
				script {
					try {
						sh 'mkdir ${REPO_DIR_NAME}'
					} catch (err) {
						dir("${REPO_DIR_NAME}") {
							deleteDir()
						}
						sh 'mkdir ${REPO_DIR_NAME}'
					}
				}

				dir("${REPO_DIR_NAME}") {
					git url: 'git@github.com:QualityObjects/qo-java-commons.git',
						branch: '${BUILD_BRANCH}',
						credentialsId: gitCredentialsId
				}
				sh 'ls'
				sh 'mkdir -p .cache/.m2 && sleep 1'
				withCredentials([file(credentialsId: mavenCredentialsId, variable: 'MVN_SETTINGS')]) {
//					sh 'mkdir -p .cache/.m2 | cp "$MVN_SETTINGS" .cache/.m2/settings.xml && chmod 600 .cache/.m2/settings.xml'
                  sh 'cp "$MVN_SETTINGS" .cache/.m2/settings.xml && chmod 600 .cache/.m2/settings.xml'
				}
			}
		}

		stage('Test') {
            when {
                expression {
                    return env.LIB_TESTS != 'no_tests';
                }
            }
            stages {
                stage('Unit tests') {
                    steps {
                        dir("${REPO_DIR_NAME}") {
                            sh 'mvn verify'
                        }
                    }
                }
                stage('Sonar tests') {
                    when {
                        anyOf {
                            environment name: 'LIB_TESTS', value: 'unit_and_sonar'
                            environment name: 'BACK_TESTS', value: 'unit_and_sonar_strict_tests'
                        }
                    }
                    steps {
                        dir("${REPO_DIR_NAME}") {
                            withSonarQubeEnv(installationName: 'QO Sonar') {
                                sh 'mvn verify -U sonar:sonar -DskipTests=true'
                            }
                        }
                    }
                }
                stage('Quality Gate') {
                    when {
                        environment name: 'LIB_TESTS', value: 'unit_and_sonar_strict_tests'
                    }
                    steps {
                        waitForQualityGate abortPipeline: true
                    }
                }                
            }
		}

		stage('Deploy') {
			agent {
				docker {
					image 'maven:3-openjdk-11'
					args '--add-host nexus.qodev.es:${NEXUS_IP} '
					reuseNode true
				}
			}
			steps {
				dir("${REPO_DIR_NAME}") {
				sh 'ls'
				sh 'mvn deploy -DskipTests=true -U'
				dir('target') {
					sh 'cp *.jar "${WORKSPACE}/"'
					}
				}
				stash includes: '*.jar', name: 'lib-files'
			}
		}
    }
}
