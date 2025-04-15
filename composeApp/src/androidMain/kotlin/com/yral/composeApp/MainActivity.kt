package com.yral.composeApp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.yral.shared.core.AndroidPlatformResources
import com.yral.shared.core.PlatformResourcesHolder
import com.yral.shared.uniffi.generated.nativeActivityCreate

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        nativeActivityCreate()
        PlatformResourcesHolder.initialize(AndroidPlatformResources(this))
        setContent {
            Root()
        }
    }
}
