package me.urielsalis.projectpipeline.commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import me.urielsalis.projectpipeline.*
import me.urielsalis.projectpipeline.db.DatabaseService
import java.io.File
import kotlin.system.exitProcess

class Template : CliktCommand(help = "Create new template") {
    val name by argument(help = "Template name")
    val projectUrl by argument(help = "Project URL")

    override fun run() {
        if (File(".git").exists()) {
            echo("A git repo already exists here", err = true)
            exitProcess(-1)
        }

        echo("Creating $name")
        echo("  --Creating git repo")
        "git init".runCommand()
        echo("  --Creating Jenkinsfile-build")
        writeDefaultJenkinsfileBuild()
        echo("  --Creating Jenkinsfile-ami")
        writeDefaultJenkinsfileAmi(projectUrl)
        echo("  --Creating packer.json")
        writeDefaultPacker()
        echo("  --Adding files to git")
        "git add .".runCommand()
        echo("  --Creating Build job $name")
        echo("      --Response code: " + createJenkinsJob(name, getDefaultJobConfigBuild(projectUrl)))
        echo("  --Creating AMI job $name-ami")
        echo("      --Response code: " + createJenkinsJob("$name-ami", getDefaultJobConfigAmi(projectUrl)))
        echo("  --Adding to database")
        DatabaseService.newTemplate(name, projectUrl)
        echo("")
        echo("Done! Edit Jenkinsfile-build, Jenkinsfile-ami and packer.json to customize!")
    }
}