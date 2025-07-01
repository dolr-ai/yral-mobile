plugins {
    alias(libs.plugins.yral.shared.library)
}

kotlin {
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
