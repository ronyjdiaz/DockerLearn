package com.ronydiaz.dockerlearn.database

import org.jetbrains.exposed.sql.Table

object CategoriesTable : Table("categories") {
    val id = varchar("id", 36)
    val name = varchar("name", 100)
    val colorHex = varchar("color_hex", 10).default("#3498db")

    override val primaryKey = PrimaryKey(id)
}
