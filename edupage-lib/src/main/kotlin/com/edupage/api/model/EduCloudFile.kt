package com.edupage.api.model

/**
 * Represents a file uploaded to the EduPage cloud.
 */
data class EduCloudFile(
    val fileId: String,
    val fileName: String,
    val uploadPath: String
)
