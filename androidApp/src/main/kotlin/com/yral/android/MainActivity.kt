package com.yral.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.yral.shared.core.AndroidPlatformResources
import com.yral.shared.core.PlatformResourcesHolder
import com.yral.shared.uniffi.generated.initRustLogger

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initRustLogger()
        PlatformResourcesHolder.initialize(AndroidPlatformResources(this))
        setContent {
            Root()
        }
    }
}
