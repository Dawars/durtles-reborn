package com.example.durtlesreborn.data

import com.example.durtlesreborn.session.SessionSnapshot
import kotlinx.serialization.json.Json
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption

/** Where an unfinished session is kept between runs. At most one session is saved. */
interface SessionSnapshotStore {
    fun load(): SessionSnapshot?
    fun save(snapshot: SessionSnapshot)
    fun clear()
}

/** Keeps the snapshot in one file, written to a temporary file first so a crash never leaves half of it. */
class FileSessionSnapshotStore(private val file: File) : SessionSnapshotStore {
    private val json = Json { ignoreUnknownKeys = true }

    override fun load(): SessionSnapshot? {
        if (!file.exists()) return null
        val snapshot = runCatching { json.decodeFromString<SessionSnapshot>(file.readText()) }.getOrNull()
        return snapshot?.takeIf { it.version == SessionSnapshot.VERSION && it.sessionType != null }
    }

    override fun save(snapshot: SessionSnapshot) {
        val temp = File(file.parentFile, file.name + ".tmp")
        temp.writeText(json.encodeToString(SessionSnapshot.serializer(), snapshot))
        Files.move(temp.toPath(), file.toPath(), StandardCopyOption.REPLACE_EXISTING)
    }

    override fun clear() {
        file.delete()
    }
}
