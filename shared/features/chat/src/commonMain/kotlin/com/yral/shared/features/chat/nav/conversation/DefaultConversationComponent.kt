package com.yral.shared.features.chat.nav.conversation

import com.arkivanov.decompose.ComponentContext
import com.yral.shared.analytics.events.SignupPageName
import com.yral.shared.features.auth.ui.LoginBottomSheetType
import com.yral.shared.rust.service.utils.CanisterData
import org.koin.core.component.KoinComponent

internal class DefaultConversationComponent(
    componentContext: ComponentContext,
    override val influencerId: String,
    private val onBack: () -> Unit,
    override val openProfile: (userCanisterData: CanisterData) -> Unit,
    private val showLoginBottomSheet: (
        pageName: SignupPageName,
        loginBottomSheetType: LoginBottomSheetType,
        onDismissRequest: () -> Unit,
        onLoginSuccess: () -> Unit,
    ) -> Unit,
    private val hideLoginBottomSheetIfVisible: () -> Unit,
) : ConversationComponent(),
    ComponentContext by componentContext,
    KoinComponent {
    override fun onBack() {
        onBack.invoke()
    }

    override fun showLoginBottomSheet(
        pageName: SignupPageName,
        loginBottomSheetType: LoginBottomSheetType,
        onDismissRequest: () -> Unit,
        onLoginSuccess: () -> Unit,
    ) {
        showLoginBottomSheet.invoke(
            pageName,
            loginBottomSheetType,
            onDismissRequest,
            onLoginSuccess,
        )
    }

    override fun hideLoginBottomSheetIfVisible() {
        hideLoginBottomSheetIfVisible.invoke()
    }
}
