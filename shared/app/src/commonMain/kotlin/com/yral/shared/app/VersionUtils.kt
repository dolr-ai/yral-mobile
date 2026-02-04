package com.yral.shared.app

fun isVersionLower(
    current: String,
    min: String,
): Boolean {
    val currentParts = current.split(".").map { it.toIntOrNull() ?: 0 }
    val minParts = min.split(".").map { it.toIntOrNull() ?: 0 }
    val maxLength = maxOf(currentParts.size, minParts.size)
    for (i in 0 until maxLength) {
        val c = currentParts.getOrElse(i) { 0 }
        val m = minParts.getOrElse(i) { 0 }
        if (c != m) return c < m
    }
    return false
}
