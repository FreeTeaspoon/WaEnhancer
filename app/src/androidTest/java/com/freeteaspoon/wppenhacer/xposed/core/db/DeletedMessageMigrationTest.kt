package com.freeteaspoon.wppenhacer.xposed.core.db

import android.database.sqlite.SQLiteDatabase
import androidx.test.platform.app.InstrumentationRegistry
import com.freeteaspoon.wppenhacer.xposed.core.db.entity.DeletedMessageBackupEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class DeletedMessageMigrationTest {
    @Test
    fun version14KeepsExistingMessagesAndAddsUsableBackups() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val name = "migration-test-${java.util.UUID.randomUUID()}.db"
        val path = context.getDatabasePath(name)
        path.parentFile!!.mkdirs()
        try {
            SQLiteDatabase.openOrCreateDatabase(path, null).use { old ->
                old.execSQL("CREATE TABLE delmessages (_id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, jid TEXT, msgid TEXT, timestamp INTEGER DEFAULT 0)")
                old.execSQL("CREATE UNIQUE INDEX index_delmessages_jid_msgid ON delmessages (jid, msgid)")
                old.execSQL("CREATE INDEX idx_delmessages_msgid ON delmessages (msgid)")
                old.execSQL("INSERT INTO delmessages (jid, msgid, timestamp) VALUES ('chat', 'existing', 123)")
                old.version = 14
            }
            val migrated = DelMessageDatabase.createDatabaseBuilder(context, name).build()
            try {
                val dao = migrated.delMessageDao()
                assertEquals(listOf("existing"), dao.getMessagesByJid("chat"))
                assertEquals(123L, dao.getTimestampByMessageId("existing"))
                assertEquals(15, migrated.openHelper.readableDatabase.version)
                dao.upsertDeletedMessageBackup(DeletedMessageBackupEntity(
                    id = 1, deletedAt = 100, messageUniqueHash = "unique", keyId = "backup",
                    messageJson = "{}"
                ))
                assertNotNull(dao.getBackupById(1))
                assertEquals("backup", dao.getBackupById(1)?.keyId)
                assertEquals(1, dao.deleteBackupById(1))
            } finally {
                migrated.close()
            }
        } finally {
            context.deleteDatabase(name)
        }
    }
}
