package com.edupage.api.modules

import com.edupage.api.EdupageSession
import com.edupage.api.exceptions.NotLoggedInException
import com.edupage.api.model.EduCloudFile
import com.google.gson.JsonParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File

internal class Cloud(private val session: EdupageSession) {

    suspend fun uploadFile(file: File): EduCloudFile {
        if (!session.isLoggedIn) throw NotLoggedInException()

        return withContext(Dispatchers.IO) {
            val url = "https://${session.subdomain}.edupage.org/timeline/server/cloud.js?__func=uploadCloudFile"

            val mimeType = detectMimeType(file)
            val fileBody = file.asRequestBody(mimeType.toMediaType())
            val multipart = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("file", file.name, fileBody)
                .addFormDataPart("__gsh", session.gsecHash ?: "")
                .build()

            val request = Request.Builder().url(url).post(multipart).build()
            val response = session.httpClient.newCall(request).execute()
            val responseStr = response.body?.string() ?: throw RuntimeException("Empty upload response")

            val json = JsonParser.parseString(responseStr).asJsonObject
            val r = json.getAsJsonObject("r") ?: throw RuntimeException("Invalid upload response")

            EduCloudFile(
                fileId = r.get("id")?.asString ?: "",
                fileName = file.name,
                uploadPath = r.get("path")?.asString ?: ""
            )
        }
    }

    private fun detectMimeType(file: File): String {
        return when (file.extension.lowercase()) {
            "pdf" -> "application/pdf"
            "jpg", "jpeg" -> "image/jpeg"
            "png" -> "image/png"
            "gif" -> "image/gif"
            "doc" -> "application/msword"
            "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
            "xls" -> "application/vnd.ms-excel"
            "xlsx" -> "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
            "txt" -> "text/plain"
            "zip" -> "application/zip"
            else -> "application/octet-stream"
        }
    }
}
