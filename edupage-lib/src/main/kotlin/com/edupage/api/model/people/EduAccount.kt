package com.edupage.api.model.people

import java.time.LocalDateTime

open class EduAccount(
    val personId: Int,
    val name: String,
    val gender: Gender?,
    val inSchoolSince: LocalDateTime?,
    val accountType: EduAccountType
) {
    open fun getId(): String = "${accountType.value}$personId"

    override fun toString(): String = "EduAccount(personId=$personId, name='$name', type=$accountType)"
}

