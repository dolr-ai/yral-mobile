package com.yral.shared.features.subscriptions.nav

import com.arkivanov.decompose.ComponentContext
import com.arkivanov.decompose.router.stack.ChildStack
import com.arkivanov.decompose.value.Value
import com.yral.shared.features.subscriptions.nav.main.SubscriptionsMainComponent
import com.yral.shared.libs.arch.nav.HomeChildSnapshotProvider
import kotlinx.serialization.Serializable

abstract class SubscriptionsComponent : HomeChildSnapshotProvider {
    abstract val stack: Value<ChildStack<*, Child>>

    abstract fun onBackClicked(): Boolean

    abstract val showBackIcon: Boolean
    abstract val onBack: () -> Unit

    sealed class Child {
        class Main(
            val component: SubscriptionsMainComponent,
        ) : Child()
    }

    @Serializable
    data class Snapshot(
        val routes: List<Route>,
    ) {
        @Serializable
        enum class Route { Main, }
    }

    companion object Companion {
        operator fun invoke(
            componentContext: ComponentContext,
            snapshot: Snapshot?,
            showBackIcon: Boolean = false,
            onBack: () -> Unit = {},
        ): SubscriptionsComponent =
            DefaultSubscriptionsComponent(
                componentContext = componentContext,
                snapshot = snapshot,
                showBackIcon = showBackIcon,
                onBack = onBack,
            )
    }
}
