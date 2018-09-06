package me.urielsalis.projectpipeline

import java.io.File

fun writeDefaultPacker() {
    File("packer.json").writeText("""
            {
              "variables": {
                "aws_access_key": "",
                "aws_secret_key": "",
                "node_version": "8.11.4",
                "name": ""
              },
              "builders": [
                {
                  "type": "amazon-ebs",
                  "access_key": "{{user `aws_access_key`}}",
                  "secret_key": "{{user `aws_secret_key`}}",
                  "region": "us-east-2",
                  "source_ami_filter": {
                    "filters": {
                      "virtualization-type": "hvm",
                      "name": "ubuntu/images/*ubuntu-xenial-16.04-amd64-server-*",
                      "root-device-type": "ebs"
                    },
                    "most_recent": true
                  },
                  "instance_type": "t2.micro",
                  "ssh_username": "ubuntu",
                  "ami_name": "{{user `name`}}"
                }
              ],
              "provisioners": [
                {
                  "type": "shell",
                  "inline": [
                    "mkdir /home/ubuntu/deploy"
                  ]
                },
                {
                  "type": "file",
                  "source": "./bundle/",
                  "destination": "/home/ubuntu/deploy"
                },
                {
                  "type": "shell",
                  "script": "./bundle/.pipeline/setup.sh"
                },
                {
                  "type": "shell",
                  "inline": [
                    "chmod +x /home/ubuntu/deploy/.pipeline/startup.sh",
                    "echo \"@reboot /home/ubuntu/deploy/.pipeline/startup.sh\" | crontab -"
                  ]
                }
              ]
            }
        """.trimIndent())
}

fun writeDefaultJenkinsfileAmi(projectUrl: String) {
    File("Jenkinsfile-ami").writeText("""
            pipeline {
              agent any

              tools {nodejs "node"}

              parameters {
                string(name: 'NAME', description: 'Filename that bundles were saved as')
              }

              stages {
                stage('Get template sources') {
                  steps {
                    git "$projectUrl.git"
                    sh 'git clean -fdx'
                  }
                }

                stage('Packer build script') {
                    steps {
                       step([  ${'$'}class: 'CopyArtifact',
                              filter: "${'$'}{env.NAME}.zip",
                              fingerprintArtifacts: true,
                              projectName: 'nodejs-template'
                        ])
                        unzip zipFile: "${'$'}{env.NAME}.zip", dir: './bundle'
                        sh "packer build -var 'name=${'$'}{env.NAME}' packer.json"
                    }
                }
              }
            }
        """.trimIndent())
}

fun writeDefaultJenkinsfileBuild() {
    File("Jenkinsfile-build").writeText("""
            pipeline {
              agent any

              tools {nodejs "node"}

              parameters {
                string(name: 'REPO', description: 'Repo to clone')
                string(name: 'NAME', description: 'Filename to save bundles as')
              }

              stages {
                stage('Get sources') {
                  steps {
                    git "${'$'}{env.REPO}.git"
                    sh 'git clean -fdx'
                  }
                }

                stage('Build') {
                  steps {
                    sh 'npm prune'
                    sh 'npm install'
                  }
                }

                stage('Test') {
                  steps {
                    sh 'npm test'
                  }
                }

                stage('Create build artifact') {
                  steps {
                    zip zipFile: "${'$'}{env.NAME}.zip", archive: true
                  }
                }
              }
            }
        """.trimIndent())
}

fun getDefaultJobConfigBuild(projectUrl: String): String {
    return """
        <?xml version='1.1' encoding='UTF-8'?>
        <flow-definition plugin="workflow-job@2.24">
            <actions>
                <org.jenkinsci.plugins.pipeline.modeldefinition.actions.DeclarativeJobAction plugin="pipeline-model-definition@1.3.2"/>
                <org.jenkinsci.plugins.pipeline.modeldefinition.actions.DeclarativeJobPropertyTrackerAction plugin="pipeline-model-definition@1.3.2">
                    <jobProperties/>
                    <triggers/>
                    <parameters>
                        <string>REPO</string>
                        <string>NAME</string>
                    </parameters>
                    <options/>
                </org.jenkinsci.plugins.pipeline.modeldefinition.actions.DeclarativeJobPropertyTrackerAction>
            </actions>
            <description></description>
            <keepDependencies>false</keepDependencies>
            <properties>
                <hudson.model.ParametersDefinitionProperty>
                    <parameterDefinitions>
                        <hudson.model.StringParameterDefinition>
                            <name>REPO</name>
                            <description>Repo to clone</description>
                            <trim>false</trim>
                        </hudson.model.StringParameterDefinition>
                        <hudson.model.StringParameterDefinition>
                            <name>NAME</name>
                            <description>Filename to save bundles as</description>
                            <trim>false</trim>
                        </hudson.model.StringParameterDefinition>
                    </parameterDefinitions>
                </hudson.model.ParametersDefinitionProperty>
            </properties>
            <definition class="org.jenkinsci.plugins.workflow.cps.CpsScmFlowDefinition" plugin="workflow-cps@2.54">
                <scm class="hudson.plugins.git.GitSCM" plugin="git@3.9.1">
                    <configVersion>2</configVersion>
                    <userRemoteConfigs>
                        <hudson.plugins.git.UserRemoteConfig>
                            <url>$projectUrl.git</url>
                        </hudson.plugins.git.UserRemoteConfig>
                    </userRemoteConfigs>
                    <branches>
                        <hudson.plugins.git.BranchSpec>
                            <name>*/master</name>
                        </hudson.plugins.git.BranchSpec>
                    </branches>
                    <doGenerateSubmoduleConfigurations>false</doGenerateSubmoduleConfigurations>
                    <submoduleCfg class="list"/>
                    <extensions/>
                </scm>
                <scriptPath>Jenkinsfile-build</scriptPath>
                <lightweight>true</lightweight>
            </definition>
            <triggers/>
            <disabled>false</disabled>
        </flow-definition>
    """.trimIndent()
}

fun getDefaultJobConfigAmi(projectUrl: String): String {
    return """
        <?xml version='1.1' encoding='UTF-8'?>
        <flow-definition plugin="workflow-job@2.24">
            <actions>
                <org.jenkinsci.plugins.pipeline.modeldefinition.actions.DeclarativeJobAction plugin="pipeline-model-definition@1.3.2"/>
                <org.jenkinsci.plugins.pipeline.modeldefinition.actions.DeclarativeJobPropertyTrackerAction plugin="pipeline-model-definition@1.3.2">
                    <jobProperties/>
                    <triggers/>
                    <parameters>
                        <string>NAME</string>
                    </parameters>
                    <options/>
                </org.jenkinsci.plugins.pipeline.modeldefinition.actions.DeclarativeJobPropertyTrackerAction>
            </actions>
            <description></description>
            <keepDependencies>false</keepDependencies>
            <properties>
                <hudson.model.ParametersDefinitionProperty>
                    <parameterDefinitions>
                        <hudson.model.StringParameterDefinition>
                            <name>NAME</name>
                            <description>Filename that bundles were saved as</description>
                            <trim>false</trim>
                        </hudson.model.StringParameterDefinition>
                    </parameterDefinitions>
                </hudson.model.ParametersDefinitionProperty>
            </properties>
            <definition class="org.jenkinsci.plugins.workflow.cps.CpsScmFlowDefinition" plugin="workflow-cps@2.54">
                <scm class="hudson.plugins.git.GitSCM" plugin="git@3.9.1">
                    <configVersion>2</configVersion>
                    <userRemoteConfigs>
                        <hudson.plugins.git.UserRemoteConfig>
                            <url>$projectUrl.git</url>
                        </hudson.plugins.git.UserRemoteConfig>
                    </userRemoteConfigs>
                    <branches>
                        <hudson.plugins.git.BranchSpec>
                            <name>*/master</name>
                        </hudson.plugins.git.BranchSpec>
                    </branches>
                    <doGenerateSubmoduleConfigurations>false</doGenerateSubmoduleConfigurations>
                    <submoduleCfg class="list"/>
                    <extensions/>
                </scm>
                <scriptPath>Jenkinsfile-ami</scriptPath>
                <lightweight>true</lightweight>
            </definition>
            <triggers/>
            <disabled>false</disabled>
        </flow-definition>
    """.trimIndent()
}

fun getDefaultConfigApp(template: String, projectUrl: String, name: String): String {
    return """<?xml version='1.1' encoding='UTF-8'?>
                <flow-definition plugin="workflow-job@2.24">
                    <actions>
                        <org.jenkinsci.plugins.pipeline.modeldefinition.actions.DeclarativeJobAction plugin="pipeline-model-definition@1.3.2"/>
                        <org.jenkinsci.plugins.pipeline.modeldefinition.actions.DeclarativeJobPropertyTrackerAction plugin="pipeline-model-definition@1.3.2">
                            <jobProperties/>
                            <triggers/>
                            <parameters/>
                            <options/>
                        </org.jenkinsci.plugins.pipeline.modeldefinition.actions.DeclarativeJobPropertyTrackerAction>
                    </actions>
                    <description>Auto generated from template $template</description>
                    <keepDependencies>false</keepDependencies>
                    <properties>
                        <org.jenkinsci.plugins.workflow.job.properties.PipelineTriggersJobProperty>
                            <triggers>
                                <com.cloudbees.jenkins.GitHubPushTrigger plugin="github@1.29.2">
                                    <spec></spec>
                                </com.cloudbees.jenkins.GitHubPushTrigger>
                            </triggers>
                        </org.jenkinsci.plugins.workflow.job.properties.PipelineTriggersJobProperty>
                        <com.coravy.hudson.plugins.github.GithubProjectProperty plugin="github@1.29.2">
                            <projectUrl>$projectUrl</projectUrl>
                            <displayName>$name</displayName>
                        </com.coravy.hudson.plugins.github.GithubProjectProperty>
                    </properties>
                    <definition class="org.jenkinsci.plugins.workflow.cps.CpsScmFlowDefinition" plugin="workflow-cps@2.54">
                        <scm class="hudson.plugins.git.GitSCM" plugin="git@3.9.1">
                            <configVersion>2</configVersion>
                            <userRemoteConfigs>
                                <hudson.plugins.git.UserRemoteConfig>
                                    <url>$projectUrl.git</url>
                                </hudson.plugins.git.UserRemoteConfig>
                            </userRemoteConfigs>
                            <branches>
                                <hudson.plugins.git.BranchSpec>
                                    <name>*/master</name>
                                </hudson.plugins.git.BranchSpec>
                            </branches>
                            <doGenerateSubmoduleConfigurations>false</doGenerateSubmoduleConfigurations>
                            <submoduleCfg class="list"/>
                            <extensions/>
                        </scm>
                        <scriptPath>.pipeline/Jenkinsfile</scriptPath>
                        <lightweight>false</lightweight>
                    </definition>
                    <triggers/>
                    <disabled>false</disabled>
                </flow-definition>
            """.trimIndent()
}