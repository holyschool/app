package com.edupage.api.model.people

import java.time.LocalDateTime

class EduParent(
    personId: Int,
    name: String,
    gender: Gender?,
    inSchoolSince: LocalDateTime?
) : EduAccount(personId, name, gender, inSchoolSince, EduAccountType.PARENT)
