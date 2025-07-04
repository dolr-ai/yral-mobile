plugins {
    alias(libs.plugins.yral.shared.library)
    alias(libs.plugins.yral.android.library)
}

kotlin {
    androidTarget()
    listOf(
        iosArm64(),
        iosSimulatorArm64(),
    )

    sourceSets {
        commonMain {
            dependencies {
                implementation(libs.kotlinx.coroutines.core)
                api(libs.kotlinResult.core)
                implementation(projects.shared.libs.coroutinesX)
                implementation(libs.essenty.instanceKeeper)
                implementation(libs.essenty.lifecycle)
                implementation(libs.essenty.lifecycle.coroutines)
            }
        }
    }
}
