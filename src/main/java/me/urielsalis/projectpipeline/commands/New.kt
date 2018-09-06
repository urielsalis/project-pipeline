package me.urielsalis.projectpipeline.commands

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.parameters.arguments.argument
import me.urielsalis.projectpipeline.createJenkinsJob
import me.urielsalis.projectpipeline.db.DatabaseService
import me.urielsalis.projectpipeline.getDefaultConfigApp
import me.urielsalis.projectpipeline.runCommand
import java.io.File
import kotlin.system.exitProcess

class New : CliktCommand(help = "Create new pipeline from template") {
    val name by argument(help = "Job name")
    val template by argument(help = "Template name")
    val projectUrl by argument(help = "Project URL")

    override fun run() {
        if (!File(".git").exists()) {
            echo("This has to be run from the root folder of a git repo", err = true)
            exitProcess(-1)
        }
        val pipelineFolder = File(".pipeline")
        if (!pipelineFolder.mkdir()) {
            echo("Couldn't create .pipeline directory, it already exists?", err = true)
            exitProcess(-1)
        }

        val template = DatabaseService.getTemplate(template)
        if (template == null) {
            echo("$template not found", err = true)
            exitProcess(-1)
        }

        echo("Creating $name from ${template.name}")

        echo("  --Cloning template from ${template.git}.git")
        "git clone ${template.git}.git temp".runCommand()

        val templateDir = File("temp")
        echo("  --Writing Jenkinsfile")
        File(pipelineFolder, "Jenkinsfile").writeText(File(templateDir, "Jenkinsfile").readText().replaceVariables(template.name, name, projectUrl))

        echo("  --Writing setup.sh")
        File(pipelineFolder, "setup.sh").writeText(File(templateDir, "setup.sh").readText().replaceVariables(template.name, name, projectUrl))


        echo("  --Writing startup.sh")
        File(pipelineFolder, "startup.sh").writeText(File(templateDir, "startup.sh").readText().replaceVariables(template.name, name, projectUrl))

        echo("  --Adding to git repository")
        "git add .pipeline".runCommand()

        echo("  --Creating job in jenkins")
        createJenkinsJob(name, getDefaultConfigApp(template.name, projectUrl, name))

        echo("  --Adding to database")
        DatabaseService.newApp(name, projectUrl, template)

        echo("")
        echo("Done! Edit setup.sh and startup.sh to customize")
    }
}

private fun String.replaceVariables(templateName: String, name: String, projectUrl: String): String =
        this.replace("\$template", templateName).replace("\$projectUrl", projectUrl).replace("\$name", name)
