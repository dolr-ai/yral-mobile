import com.yral.buildlogic.configureIosTargets
plugins {
    alias(libs.plugins.yral.shared.library)
    alias(libs.plugins.yral.android.library)
}

kotlin {
    androidTarget()
    configureIosTargets(project)

    sourceSets {
        commonMain.dependencies {
            implementation(projects.shared.libs.analytics)
            implementation(projects.shared.libs.featureFlag)
            implementation(libs.kotlinx.coroutines.core)
            implementation(projects.shared.libs.arch)
            implementation(projects.shared.libs.preferences)
            implementation(projects.shared.rust.service)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
        }
    }
}
