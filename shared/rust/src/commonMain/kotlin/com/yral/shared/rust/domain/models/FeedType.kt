package com.yral.shared.rust.domain.models

enum class FeedType(
    val value: String,
) {
    CURRENT_USER("currentUser"),
    OTHER_USERS("otherUsers"),
    ;

    override fun toString(): String = value
}
