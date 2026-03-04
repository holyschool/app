package com.edupage.api.model.people

import java.time.LocalDateTime

/**
 * Represents a parent account.
 */
class EduParent(
    personId: Int,
    name: String,
    gender: Gender?,
    inSchoolSince: LocalDateTime?
) : EduAccount(personId, name, gender, inSchoolSince, EduAccountType.PARENT)
