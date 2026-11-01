package com.example.durtlesreborn.data

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

private const val BATCH = 300

/** Adds the searchable text column and fills it for every stored subject. */
val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE subject ADD COLUMN searchText TEXT NOT NULL DEFAULT ''")
        var lastId = -1L
        while (true) {
            // Read in small batches: the subject JSON is large, and writing while a cursor is open is avoided.
            val batch = ArrayList<Pair<Long, String>>(BATCH)
            db.query("SELECT id, characters, slug, json FROM subject WHERE id > $lastId ORDER BY id LIMIT $BATCH").use { c ->
                while (c.moveToNext()) {
                    val text = buildSearchText(
                        characters = if (c.isNull(1)) null else c.getString(1),
                        slug = if (c.isNull(2)) null else c.getString(2),
                        content = SubjectContent.parse(c.getString(3)),
                    )
                    batch += c.getLong(0) to text
                }
            }
            if (batch.isEmpty()) break
            for ((id, text) in batch) db.execSQL("UPDATE subject SET searchText = ? WHERE id = ?", arrayOf<Any>(text, id))
            lastId = batch.last().first
        }
    }

}
