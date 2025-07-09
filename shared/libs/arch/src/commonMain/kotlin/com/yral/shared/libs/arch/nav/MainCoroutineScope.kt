package com.yral.shared.libs.arch.nav

import com.arkivanov.essenty.lifecycle.LifecycleOwner
import com.arkivanov.essenty.lifecycle.coroutines.coroutineScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

// The scope is automatically cancelled when the component is destroyed
fun LifecycleOwner.mainCoroutineScope(): CoroutineScope =
    coroutineScope(
        context = Dispatchers.Main.immediate + SupervisorJob(),
    )
