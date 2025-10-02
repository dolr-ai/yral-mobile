package com.yral.android.ui.screens.uploadVideo

import androidx.compose.foundation.background
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.arkivanov.decompose.extensions.compose.stack.Children
import com.arkivanov.decompose.extensions.compose.stack.animation.slide
import com.arkivanov.decompose.extensions.compose.stack.animation.stackAnimation
import com.yral.android.ui.screens.account.LoginBottomSheet
import com.yral.shared.features.auth.viewModel.LoginViewModel
import com.yral.shared.features.uploadvideo.nav.UploadVideoRootComponent
import com.yral.shared.features.uploadvideo.ui.FlowSelectionScreen
import com.yral.shared.features.uploadvideo.ui.aiVideoGen.AiVideoGenScreen
import com.yral.shared.features.uploadvideo.ui.fileUpload.UploadVideoScreen
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UploadVideoRootScreen(
    component: UploadVideoRootComponent,
    bottomPadding: Dp,
) {
    Children(
        stack = component.stack,
        modifier = Modifier.background(MaterialTheme.colorScheme.primaryContainer),
        animation = stackAnimation(slide()),
    ) { child ->
        when (val instance = child.instance) {
            is UploadVideoRootComponent.Child.FlowSelection -> {
                FlowSelectionScreen(component = instance.component)
            }
            is UploadVideoRootComponent.Child.AiVideoGen -> {
                val loginViewModel: LoginViewModel = koinViewModel()
                val loginState by loginViewModel.state.collectAsStateWithLifecycle()
                AiVideoGenScreen(
                    component = instance.component,
                    bottomPadding = bottomPadding,
                    loginState = loginState,
                    loginBottomSheet = { pageName, bottomSheetState, onDismissRequest, termsLink, openTerms ->
                        LoginBottomSheet(
                            bottomSheetState = bottomSheetState,
                            onDismissRequest = onDismissRequest,
                            termsLink = termsLink,
                            openTerms = openTerms,
                        )
                    },
                )
            }
            is UploadVideoRootComponent.Child.FileUpload -> {
                UploadVideoScreen(component = instance.component, bottomPadding = bottomPadding)
            }
        }
    }
}
