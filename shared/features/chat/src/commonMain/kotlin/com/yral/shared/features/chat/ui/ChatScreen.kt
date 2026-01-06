package com.yral.shared.features.chat.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import com.arkivanov.decompose.extensions.compose.stack.Children
import com.arkivanov.decompose.extensions.compose.stack.animation.slide
import com.arkivanov.decompose.extensions.compose.stack.animation.stackAnimation
import com.yral.shared.features.chat.nav.ChatComponent
import com.yral.shared.features.chat.ui.conversation.ChatConversationScreen
import com.yral.shared.features.chat.ui.wall.ChatWallScreen
import com.yral.shared.features.chat.viewmodel.ChatWallViewModel
import com.yral.shared.features.chat.viewmodel.ConversationViewModel
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    component: ChatComponent,
    chatWallViewModel: ChatWallViewModel = koinViewModel(),
    conversationViewModel: ConversationViewModel = koinViewModel(),
    modifier: Modifier = Modifier,
    conversationModifier: Modifier = Modifier,
    bottomPadding: Dp,
) {
    Children(
        stack = component.stack,
        animation = stackAnimation(slide()),
        modifier = modifier,
    ) { child ->
        when (val instance = child.instance) {
            is ChatComponent.Child.Wall ->
                ChatWallScreen(
                    component = instance.component,
                    viewModel = chatWallViewModel,
                    modifier = Modifier.fillMaxSize(),
                )
            is ChatComponent.Child.Conversation ->
                ChatConversationScreen(
                    component = instance.component,
                    viewModel = conversationViewModel,
                    modifier = conversationModifier.fillMaxSize(),
                    bottomPadding = bottomPadding,
                )
        }
    }
}
