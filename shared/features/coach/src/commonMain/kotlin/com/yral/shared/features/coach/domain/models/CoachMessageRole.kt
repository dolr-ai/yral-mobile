package com.yral.shared.features.coach.domain.models

enum class CoachMessageRole {
    CREATOR,
    COACH,
    UNKNOWN,
    ;

    val apiValue: String
        get() =
            when (this) {
                CREATOR -> "creator"
                COACH -> "coach"
                UNKNOWN -> "unknown"
            }

    companion object {
        fun fromApi(api: String?): CoachMessageRole =
            when (api?.lowercase()) {
                "creator" -> CREATOR
                "coach" -> COACH
                else -> UNKNOWN
            }
    }
}
