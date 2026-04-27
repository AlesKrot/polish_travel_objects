package com.aleskrot.server.database

import org.jetbrains.exposed.sql.Table

object HeritageTable : Table("heritage_items") {
    val id = integer("id").autoIncrement()
    val item = varchar("item", 500)
    val itemLabel = varchar("item_label", 255)
    val coords = varchar("coords", 100)
    val categoryLabel = varchar("category_label", 255)
    val image = varchar("image", 500)

    override val primaryKey = PrimaryKey(id)
}