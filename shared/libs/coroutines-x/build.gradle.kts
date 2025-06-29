plugins {
    alias(libs.plugins.yral.shared.library)
}

kotlin {
    sourceSets {
        commonMain {
            dependencies {
                api(libs.kotlinx.coroutines.core)
            }
        }
    }
}
