package com.yral.shared.features.coach.nav

import com.arkivanov.decompose.ComponentContext
import org.koin.core.component.KoinComponent

internal class DefaultCoachComponent(
    componentContext: ComponentContext,
    override val openCoachParams: OpenCoachParams,
    private val onBack: () -> Unit,
    private val openSoulFile: (OpenSoulFileParams) -> Unit,
) : CoachComponent(),
    ComponentContext by componentContext,
    KoinComponent {
    override fun onBack() {
        onBack.invoke()
    }

    override fun openSoulFile() {
        openSoulFile.invoke(
            OpenSoulFileParams(
                botId = openCoachParams.botId,
                botName = openCoachParams.botName,
                avatarUrl = openCoachParams.avatarUrl,
            ),
        )
    }
}
