package com.yral.shared.features.chat.nav.conversation

import com.arkivanov.decompose.ComponentContext
import com.yral.shared.analytics.events.SignupPageName
import com.yral.shared.features.auth.ui.LoginBottomSheetType
import com.yral.shared.rust.service.utils.CanisterData

abstract class ConversationComponent {
    abstract val influencerId: String
    abstract val influencerCategory: String
    abstract val openProfile: (userCanisterData: CanisterData) -> Unit
    abstract fun onBack()
    abstract fun showLoginBottomSheet(
        pageName: SignupPageName,
        loginBottomSheetType: LoginBottomSheetType,
        onDismissRequest: () -> Unit,
        onLoginSuccess: () -> Unit,
    )

    abstract fun hideLoginBottomSheetIfVisible()

    companion object Companion {
        operator fun invoke(
            componentContext: ComponentContext,
            influencerId: String,
            influencerCategory: String,
            onBack: () -> Unit,
            openProfile: (userCanisterData: CanisterData) -> Unit,
            showLoginBottomSheet: (
                pageName: SignupPageName,
                loginBottomSheetType: LoginBottomSheetType,
                onDismissRequest: () -> Unit,
                onLoginSuccess: () -> Unit,
            ) -> Unit = { _, _, _, _ -> },
            hideLoginBottomSheetIfVisible: () -> Unit = {},
        ): ConversationComponent =
            DefaultConversationComponent(
                componentContext = componentContext,
                influencerId = influencerId,
                influencerCategory = influencerCategory,
                onBack = onBack,
                openProfile = openProfile,
                showLoginBottomSheet = showLoginBottomSheet,
                hideLoginBottomSheetIfVisible = hideLoginBottomSheetIfVisible,
            )
    }
}
