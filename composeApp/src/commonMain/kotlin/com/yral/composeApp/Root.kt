package com.yral.composeApp

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.yral.shared.core.Greeting
import com.yral.shared.features.auth.DefaultAuthClient
import com.yral.shared.koin.koinInstance
import com.yral.shared.rust.domain.IndividualUserRepository
import com.yral.shared.rust.services.IndividualUserServiceFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

@Suppress("LongMethod")
@Composable
fun Root() {
    val defaultAuthClient: DefaultAuthClient = remember { koinInstance.get() }
    val individualUserRepository: IndividualUserRepository = remember { koinInstance.get() }
    val individualUserServiceFactory: IndividualUserServiceFactory = remember { koinInstance.get() }
    var initialised by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        withContext(Dispatchers.IO) {
            defaultAuthClient.initialize()
            defaultAuthClient.canisterPrincipal?.let { principal ->
                defaultAuthClient.identity?.let { identity ->
                    individualUserServiceFactory.initialize(
                        principal = principal,
                        identityData = identity,
                    )
                }
            }
            initialised = true
        }
    }
    LaunchedEffect(initialised) {
        if (defaultAuthClient.canisterPrincipal != null) {
            withContext(Dispatchers.IO) {
                val posts =
                    defaultAuthClient.canisterPrincipal?.let {
                        individualUserRepository.getPostsOfThisUserProfileWithPaginationCursor(
                            pageNo = 0UL,
                        )
                    }
                println("xxxx $posts")
            }
        }
    }
    MyApplicationTheme {
        Scaffold(
            modifier = Modifier.fillMaxSize(),
            containerColor = MaterialTheme.colorScheme.background,
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
        ) { innerPadding ->
            Box(
                modifier =
                    Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .consumeWindowInsets(innerPadding)
                        .windowInsetsPadding(
                            WindowInsets.safeDrawing,
                        ),
                contentAlignment = Alignment.Center,
            ) {
                Column {
                    GreetingView(Greeting().greet().plus("from Compose Multiplatform!"))
                    Spacer(Modifier.height(16.dp))
                }
            }
        }
    }
}

@Composable
fun GreetingView(text: String) {
    Text(text = text)
}
