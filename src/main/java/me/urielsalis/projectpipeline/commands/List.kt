package me.urielsalis.projectpipeline.commands

import com.github.ajalt.clikt.core.CliktCommand
import me.urielsalis.projectpipeline.db.DatabaseService
import org.jetbrains.exposed.sql.transactions.transaction

class List : CliktCommand(help = "List templates") {
    override fun run() {
        val templates = DatabaseService.getTemplates()
        transaction {
            templates.forEach {
                echo("${it.name} - ${it.git}")
                DatabaseService.getApplicationsFromTemplate(it).forEach {
                    echo("  ${it.name} - ${it.git}")
                }
            }
        }
    }

}
