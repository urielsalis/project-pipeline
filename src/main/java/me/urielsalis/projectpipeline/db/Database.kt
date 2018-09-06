package me.urielsalis.projectpipeline.db

import me.urielsalis.projectpipeline.dbPass
import me.urielsalis.projectpipeline.dbUrl
import me.urielsalis.projectpipeline.dbUser
import org.jetbrains.exposed.sql.Database
import org.jetbrains.exposed.sql.SchemaUtils.createMissingTablesAndColumns
import org.jetbrains.exposed.sql.SizedIterable
import org.jetbrains.exposed.sql.StdOutSqlLogger
import org.jetbrains.exposed.sql.addLogger
import org.jetbrains.exposed.sql.transactions.transaction

object DatabaseService {
    fun init() {
        Database.connect(dbUrl, driver = "org.postgresql.Driver", user = dbUser, password = dbPass)
        transaction {
            addLogger(StdOutSqlLogger)
            createMissingTablesAndColumns(Templates, Applications)
        }
    }

    fun getTemplates(): SizedIterable<Template> = transaction {
        addLogger(StdOutSqlLogger)
        Template.all()
    }

    fun newTemplate(name: String, projectUrl: String) = transaction {
        addLogger(StdOutSqlLogger)
        Template.new {
            this.name = name
            this.git = projectUrl
        }
    }

    fun newApp(name: String, projectUrl: String, template: Template) = transaction {
        addLogger(StdOutSqlLogger)
        Application.new {
            this.name = name
            this.git = projectUrl
            this.template = template
        }
    }

    fun getTemplate(template: String): Template? = transaction {
        Template.find { Templates.name eq template }.firstOrNull()
    }

    fun getApplicationsFromTemplate(template: Template): Iterable<Application> = transaction {
        template.jobs
    }
}