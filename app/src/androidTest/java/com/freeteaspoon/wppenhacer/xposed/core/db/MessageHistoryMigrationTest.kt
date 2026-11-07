package com.freeteaspoon.wppenhacer.xposed.core.db

import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.platform.app.InstrumentationRegistry
import com.freeteaspoon.wppenhacer.xposed.core.db.entity.HideSeenEntity
import org.junit.Assert.assertEquals
import org.junit.Test

class MessageHistoryMigrationTest {
    @Test fun version7PreservesReceiptsAndSupportsLidLookups() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val name = "history-migration-${java.util.UUID.randomUUID()}.db"
        val path = context.getDatabasePath(name)
        path.parentFile!!.mkdirs()
        try {
            SQLiteDatabase.openOrCreateDatabase(path, null).use { old ->
                old.execSQL("CREATE TABLE MessageHistory (_id INTEGER PRIMARY KEY AUTOINCREMENT, row_id INTEGER NOT NULL, text_data TEXT NOT NULL, editTimestamp INTEGER DEFAULT 0)")
                old.execSQL("CREATE INDEX idx_message_history_row_id ON MessageHistory (row_id)")
                old.execSQL("CREATE TABLE hide_seen_messages (_id INTEGER PRIMARY KEY AUTOINCREMENT, jid TEXT NOT NULL, message_id TEXT NOT NULL, read INTEGER NOT NULL DEFAULT 0, played INTEGER NOT NULL DEFAULT 0)")
                old.execSQL("CREATE UNIQUE INDEX idx_hide_seen_jid_message_unique ON hide_seen_messages (jid, message_id)")
                old.execSQL("CREATE INDEX idx_hide_seen_jid_read ON hide_seen_messages (jid, read)")
                old.execSQL("CREATE INDEX idx_hide_seen_jid_played ON hide_seen_messages (jid, played)")
                old.execSQL("CREATE TABLE device_history (_id INTEGER PRIMARY KEY AUTOINCREMENT, userjid TEXT NOT NULL, message_id TEXT NOT NULL, device_type INTEGER NOT NULL)")
                old.execSQL("CREATE UNIQUE INDEX idx_device_userjid_message_id_unique ON device_history (userjid, message_id)")
                old.execSQL("CREATE INDEX idx_device_message_id ON device_history (message_id)")
                old.execSQL("INSERT INTO hide_seen_messages (jid, message_id, read, played) VALUES ('old@s.whatsapp.net', 'existing', 1, 0)")
                old.version = 7
            }
            val migrated = Room.databaseBuilder(context, MessageHistoryDatabase::class.java, name)
                .addMigrations(MessageHistoryDatabase.MIGRATION_7_8).build()
            try {
                val dao = migrated.hideSeenDao()
                assertEquals(8, migrated.openHelper.readableDatabase.version)
                assertEquals(1, dao.getReadState(listOf("old@s.whatsapp.net"), "existing"))
                assertEquals(1, dao.updateUserJidIfNeeded("old@s.whatsapp.net", "old@lid", "existing"))
                assertEquals(1, dao.getReadState(listOf("old@lid"), "existing"))
                assertEquals(1, dao.updatePlayed(listOf("old@lid"), "existing", 1))
                assertEquals(1, dao.getPlayedState(listOf("old@s.whatsapp.net"), "existing"))
                dao.insertOrIgnore(HideSeenEntity(jid = "new@lid", userjid = "new@s.whatsapp.net", messageId = "new"))
                assertEquals(1, dao.updateReadBatch(listOf("new@s.whatsapp.net"), listOf("new"), 1))
                assertEquals(1, dao.getReadState(listOf("new@lid"), "new"))
            } finally { migrated.close() }
        } finally { context.deleteDatabase(name) }
    }
}
