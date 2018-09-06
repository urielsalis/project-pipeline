package me.urielsalis.projectpipeline.db

import org.jetbrains.exposed.dao.EntityID
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.IntIdTable

object Applications : IntIdTable() {
    val name = varchar("name", 100).index()
    val template = reference("template", Templates).index()
    val git = varchar("git", 1024)
}

class Application(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<Application>(Applications)

    var name by Applications.name
    var template by Template referencedOn Applications.template
    var git by Applications.git
}