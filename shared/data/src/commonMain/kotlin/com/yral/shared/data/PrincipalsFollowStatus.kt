package com.yral.shared.data

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

object PrincipalsFollowStatus {
    private val _principalsFollowed = MutableStateFlow<Set<String>>(setOf())
    val principalsFollowed: StateFlow<Set<String>> = _principalsFollowed

    private val _principalsUnFollowed = MutableStateFlow<Set<String>>(setOf())
    val principalsUnFollowed: StateFlow<Set<String>> = _principalsUnFollowed

    fun addPrincipal(principal: String) {
        _principalsFollowed.value += principal
        _principalsUnFollowed.value -= principal
    }

    fun removePrincipal(principal: String) {
        _principalsFollowed.value -= principal
        _principalsUnFollowed.value += principal
    }
}
