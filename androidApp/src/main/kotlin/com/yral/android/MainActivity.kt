package com.yral.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.yral.android.analytics.initAnalyticsManager
import com.yral.shared.core.AndroidPlatformResources
import com.yral.shared.core.PlatformResourcesFactory
import com.yral.shared.koin.koinInstance
import com.yral.shared.uniffi.generated.initRustLogger

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initPlatformResources()
        initRustLogger()
        initAnalyticsManager()
        setContent {
            Root()
        }
    }

    private fun initPlatformResources() {
        val platformResourcesFactory: PlatformResourcesFactory = koinInstance.get()
        platformResourcesFactory.initialize(AndroidPlatformResources(this))
    }
}
