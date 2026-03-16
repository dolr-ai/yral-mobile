import com.yral.buildlogic.configureIosTargets
plugins {
    alias(libs.plugins.yral.shared.library)
    alias(libs.plugins.yral.android.library)
}

kotlin {
    androidTarget()
    configureIosTargets(project)

    sourceSets {
        androidMain {
            dependencies {
                api(libs.koin.android)
            }
        }
        commonMain {
            dependencies {
                api(project.dependencies.platform(libs.koin.bom))
                api(libs.koin.core)
                api(libs.koin.compose)
                api(libs.koin.compose.viewmodel)
            }
        }
    }
}
