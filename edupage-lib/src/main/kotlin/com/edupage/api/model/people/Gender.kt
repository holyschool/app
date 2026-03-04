package com.edupage.api.model.people

enum class Gender(val value: String) {
    MALE("M"),
    FEMALE("F");

    companion object {
        fun parse(value: String?): Gender? = values().firstOrNull { it.value == value }
    }
}
