package me.urielsalis.projectpipeline.db

import org.jetbrains.exposed.dao.EntityID
import org.jetbrains.exposed.dao.IntEntity
import org.jetbrains.exposed.dao.IntEntityClass
import org.jetbrains.exposed.dao.IntIdTable

object Templates : IntIdTable() {
    val name = varchar("name", 100).index()
    val git = varchar("git", 1024)
}

class Template(id: EntityID<Int>) : IntEntity(id) {
    companion object : IntEntityClass<Template>(Templates)

    var name by Templates.name
    var git by Templates.git
    val jobs by Application referrersOn Applications.template
}