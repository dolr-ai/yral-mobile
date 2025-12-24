package com.yral.shared.features.chat.ui.conversation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.yral.shared.features.chat.nav.conversation.ConversationComponent
import com.yral.shared.features.chat.viewmodel.ConversationViewModel

@Composable
fun ConversationScreen(
    component: ConversationComponent,
    viewModel: ConversationViewModel,
    modifier: Modifier = Modifier,
) {
    val viewState by viewModel.viewState.collectAsState()

    Column(
        modifier =
            modifier
                .fillMaxSize()
                .padding(16.dp),
    ) {
        Text(
            text = "Conversation with ${component.influencerId}",
            modifier = Modifier.padding(bottom = 12.dp),
        )
    }
}
