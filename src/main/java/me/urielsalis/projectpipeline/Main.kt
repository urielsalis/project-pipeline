package me.urielsalis.projectpipeline

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.subcommands
import com.github.ajalt.clikt.parameters.arguments.argument
import java.io.*
import java.net.HttpURLConnection
import java.net.URL
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.system.exitProcess

fun main(args: Array<String>) = Pipeline()
        .subcommands(New(), List(), Template())
        .main(args)

class Pipeline: CliktCommand() {
    override fun run() = Unit
}

class New: CliktCommand(help="Create new pipeline from template") {
    val name by argument(help = "Job name")
    val template by argument(help = "Template name")
    val projectUrl by argument(help = "Project URL")
    val jenkinsHost by argument(help = "Jenkins host")
    val jenkinsUserPass by argument(help = "Jenkins username + access token in the format username:password")

    override fun run() {
        if(!File(".git").exists()) {
            echo("This has to be run from the root folder of a git repo", err = true)
            exitProcess(-1)
        }
        val pipelineFolder = File(".pipeline")
        if(!pipelineFolder.mkdir()) {
            echo("Couldnt create .pipeline directory, it already exists?", err = true)
            exitProcess(-1)
        }

        echo("Creating $name from $template")

        echo("  --Writing Jenkinsfile")
        File(pipelineFolder, "Jenkinsfile").writeText("""
            pipeline {
              agent any

              stages {
                stage('Build') {
                  steps {
                    build job: '$template', propagate: true, parameters: [[${'$'}class: 'StringParameterValue', name: 'REPO', value: 'https://github.com/urielsalis/node-baas'], [${'$'}class: 'StringParameterValue', name: 'NAME', value: "${'$'}{env.JOB_NAME}-${'$'}{env.BUILD_NUMBER}"]]      }
                }

                stage('Create AMI') {
                  steps {
                    build job: '$template-ami', propagate: true, parameters: [[${'$'}class: 'StringParameterValue', name: 'NAME', value: "${'$'}{env.JOB_NAME}-${'$'}{env.BUILD_NUMBER}"]]
                  }
                }
              }
            }
        """.trimIndent())

        echo("  --Writing setup.sh")
        File(pipelineFolder, "setup.sh").writeText("""
            sudo apt update
            # Run software install and extra configuration here. Everything will be deployed to /home/ubuntu/deploy, for example:
            # sudo apt install -y nodejs
            # chmod +x -R /home/ubuntu/deploy/bin
        """.trimIndent())

        echo("  --Writing startup.sh")
        File(pipelineFolder, "startup.sh").writeText("""
            # This script runs on startup. Full paths need to be used, for example:
            # /home/ubuntu/deploy/bin/baas -p 9485 -s 10
        """.trimIndent())

        echo("  --Adding to git repository")
        "git add .pipeline".runCommand()

        echo("  --Creating job in jenkins")
        with(URL("$jenkinsHost/createItem?name=$name").openConnection() as HttpURLConnection) {
            requestMethod = "POST"
            doOutput = true
            val auth = Base64.getEncoder().encode(jenkinsUserPass.toByteArray(Charsets.UTF_8)).toString(Charsets.UTF_8)

            addRequestProperty("Content-Type", "application/xml")
            addRequestProperty("Authorization", "Basic $auth")
            val wr = OutputStreamWriter(outputStream)
            wr.write("""<?xml version='1.1' encoding='UTF-8'?>
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
            """.trimIndent())
            wr.flush()

            echo("    --Response code: $responseCode")
        }

        echo("")
        echo("Done! Edit setup.sh and startup.sh to customize")
    }
}

class Template: CliktCommand(help = "Create new template") {
    val name by argument(help = "Template name")

    override fun run() {
        echo("Creating $name")
        if(!File(".git").exists()) {
            echo("This has to be run from the root folder of a git repo")
            exitProcess(-1)
        }
    }
}

class List: CliktCommand(help = "List jobs using template") {
    override fun run() {
        echo("Not implemeted yet")
    }

}

fun String.runCommand(): String? = try {
    val parts = this.split("\\s".toRegex())
    val proc = ProcessBuilder(*parts.toTypedArray())
            .redirectOutput(ProcessBuilder.Redirect.PIPE)
            .redirectError(ProcessBuilder.Redirect.PIPE)
            .start()

    proc.waitFor(60, TimeUnit.MINUTES)
    proc.inputStream.bufferedReader().readText()
} catch (e: IOException) {
    e.printStackTrace()
    null
}