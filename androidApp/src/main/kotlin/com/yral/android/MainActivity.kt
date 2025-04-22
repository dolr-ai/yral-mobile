package com.yral.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.yral.shared.core.platform.AndroidPlatformResources
import com.yral.shared.core.platform.PlatformResourcesFactory
import com.yral.shared.koin.koinInstance
import com.yral.shared.uniffi.generated.initRustLogger

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initPlatformResources()
        initRustLogger()
        setContent {
            Root()
        }
    }

    private fun initPlatformResources() {
        koinInstance
            .get<PlatformResourcesFactory>()
            .initialize(AndroidPlatformResources(this))
    }
}
