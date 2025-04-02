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
import com.yral.shared.core.PlatformResourcesHolder
import com.yral.shared.http.HttpClientFactory
import com.yral.shared.preferences.AsyncPreferencesFactory
import com.yral.shared.rust.RustGreeting
import com.yral.shared.rust.auth.DefaultAuthClient
import com.yral.shared.uniffi.generated.Result12
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

@Composable
fun Root() {
  val preferences = remember {
    AsyncPreferencesFactory.getInstance(
      platformResources = PlatformResourcesHolder.platformResources,
      ioDispatcher = Dispatchers.IO
    ).build()
  }
  val client = remember {
    HttpClientFactory.getInstance(preferences).build()
  }
  val defaultAuthClient = remember {
    DefaultAuthClient(
      preferences = preferences,
      client = client,
      ioDispatcher = Dispatchers.IO,
    )
  }
  MyApplicationTheme {
    Scaffold(
      modifier = Modifier.fillMaxSize(),
      containerColor = MaterialTheme.colorScheme.background,
      contentWindowInsets = WindowInsets(0, 0, 0, 0),
    ) { innerPadding ->
      Box(
        modifier = Modifier
          .fillMaxSize()
          .padding(innerPadding)
          .consumeWindowInsets(innerPadding)
          .windowInsetsPadding(
            WindowInsets.safeDrawing,
          ),
        contentAlignment = Alignment.Center,
      ) {
        Column {
          GreetingView(Greeting().greet())
          Spacer(Modifier.height(16.dp))
          //TraceFFIInvocation()
        }
      }
    }
    LaunchedEffect(Unit) {
      withContext(Dispatchers.IO) {
        defaultAuthClient.initialize()
      }
    }
  }
}

@Composable
fun GreetingView(text: String) {
  Text(text = text)
}

@Composable
fun TraceFFIInvocation() {
  var text by remember { mutableStateOf("") }
  Text(text = text)
  LaunchedEffect(Unit) {
    val result = withContext(Dispatchers.IO) {
      val rustGreeting = RustGreeting()
      text = rustGreeting.greet("Shivam")
      delay(1000L)
      RustGreeting().getPostsOfThisUserProfileWithPaginationCursor()
    }
    if (result is Result12.Ok) {
      text = result.v1.toString()
    } else if (result is Result12.Err) {
      text = result.v1.toString()
    }
  }
}

