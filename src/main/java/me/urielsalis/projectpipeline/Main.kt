package me.urielsalis.projectpipeline

import com.github.ajalt.clikt.core.CliktCommand
import com.github.ajalt.clikt.core.subcommands
import com.natpryce.konfig.*
import com.natpryce.konfig.ConfigurationProperties.Companion.systemProperties
import me.urielsalis.projectpipeline.commands.List
import me.urielsalis.projectpipeline.commands.New
import me.urielsalis.projectpipeline.commands.Template
import me.urielsalis.projectpipeline.db.DatabaseService
import java.io.File

val dbUrlKey = Key("db.url", stringType)
val dbUserKey = Key("db.user", stringType)
val dbPassKey = Key("db.pass", stringType)
val jenkinsHostKey = Key("jenkins.host", stringType)
val jenkinsUserKey = Key("jenkins.username", stringType)
val jenkinsAccessTokenKey = Key("jenkins.accessToken", stringType)

var dbUrl: String = ""
var dbUser: String = ""
var dbPass: String = ""
var jenkinsHost: String = ""
var jenkinsUser: String = ""
var jenkinsAccessToken: String = ""

fun main(args: Array<String>) {
    val config = systemProperties() overriding
            EnvironmentVariables() overriding
            ConfigurationProperties.fromFile(File("configuration.properties"))
    dbUrl = config[dbUrlKey]
    dbUser = config[dbUserKey]
    dbPass = config[dbPassKey]
    jenkinsAccessToken = config[jenkinsAccessTokenKey]
    jenkinsHost = config[jenkinsHostKey]
    jenkinsUser = config[jenkinsUserKey]

    DatabaseService.init()

    Pipeline()
            .subcommands(New(), List(), Template())
            .main(args)
}

class Pipeline : CliktCommand() {
    override fun run() = Unit
}