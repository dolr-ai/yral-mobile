plugins {
    alias(libs.plugins.yral.shared.library)
    alias(libs.plugins.yral.android.library)
    alias(libs.plugins.yral.shared.library.compose)
    alias(libs.plugins.composeCompiler)
}

kotlin {
    androidTarget()
    listOf(
        iosArm64(),
        iosSimulatorArm64(),
    )

    sourceSets {
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.ui)

            implementation(libs.kotlinx.coroutines.core)
        }
        androidMain.dependencies {
            implementation(libs.androidx.media3.exoplayer)
            implementation(libs.androidx.media3.exoplayer.dash)
            implementation(libs.androidx.media3.exoplayer.hls)
            implementation(libs.androidx.media3.datasource)
            implementation(libs.androidx.media3.database)
            implementation(libs.androidx.media3.ui)
            implementation(libs.androidx.media3.ui.compose.material3)
        }
    }
}

dependencies {
    debugImplementation(compose.uiTooling)
}
