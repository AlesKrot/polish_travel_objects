package com.aleskrot.server.repository

import com.aleskrot.server.database.HeritageTable
import com.aleskrot.server.models.HeritageItem
import org.jetbrains.exposed.sql.*
import org.jetbrains.exposed.sql.transactions.transaction

class HeritageRepository {

    suspend fun getAllItems(): List<HeritageItem> = transaction {
        HeritageTable.selectAll().map {
            HeritageItem(
                item = it[HeritageTable.item],
                itemLabel = it[HeritageTable.itemLabel],
                coords = it[HeritageTable.coords],
                categoryLabel = it[HeritageTable.categoryLabel],
                image = it[HeritageTable.image]
            )
        }
    }

    suspend fun addItem(item: HeritageItem) = transaction {
        HeritageTable.insert {
            it[HeritageTable.item] = item.item
            it[HeritageTable.itemLabel] = item.itemLabel
            it[HeritageTable.coords] = item.coords
            it[HeritageTable.categoryLabel] = item.categoryLabel
            it[HeritageTable.image] = item.image
        }
    }
}