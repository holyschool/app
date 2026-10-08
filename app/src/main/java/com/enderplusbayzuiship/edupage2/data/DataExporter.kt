package com.enderplusbayzuiship.edupage2.data

import com.google.gson.GsonBuilder
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DataExporter @Inject constructor(
    private val homeworkStore: LocalHomeworkStore,
) {

    fun exportAsJson(): String {
        val root = JsonObject()
        root.addProperty("app", "Edupage2")
        root.addProperty("exportedAt", System.currentTimeMillis())
        root.addProperty(
            "exportedAtIso",
            DateTimeFormatter.ISO_LOCAL_DATE_TIME.format(java.time.LocalDateTime.now())
        )
        root.addProperty("schemaVersion", 1)

        val homework = JsonArray()
        homeworkStore.getAll().forEach { item ->
            val o = JsonObject()
            o.addProperty("id", item.id)
            o.addProperty("title", item.title)
            o.addProperty("date", item.date)
            o.addProperty("subject", item.subject)
            o.addProperty("notes", item.notes)
            o.addProperty("done", item.done)
            o.addProperty("createdAtMs", item.createdAtMs)
            homework.add(o)
        }
        root.add("homework", homework)

        return GsonBuilder().setPrettyPrinting().create().toJson(root)
    }
}

