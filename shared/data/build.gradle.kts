import com.yral.buildlogic.applyCocoapodsIfApple
import com.yral.buildlogic.configureCocoapods
import com.yral.buildlogic.configureIosTargets
plugins {
    alias(libs.plugins.yral.shared.library)
    alias(libs.plugins.yral.android.library)
    alias(libs.plugins.kotlin.serialization)
}

applyCocoapodsIfApple()

configureCocoapods {
    version = "1.0"
    summary = "Shared data"
    homepage = "https://github.com/dolr-ai/yral-mobile"
    ios.deploymentTarget = "15.6"

    noPodspec()
}

kotlin {
    androidTarget()
    configureIosTargets(project)

    sourceSets {
        commonMain.dependencies {
            api(libs.kotlinx.coroutines.core)
            api(libs.kotlinResult.core)
            api(libs.kotlinResult.coroutines)
            implementation(libs.ktor.serialization.kotlinx.json)

            api(libs.kotlinResult.core)
            api(libs.kotlinResult.coroutines)
            api(libs.touchlab.logger)

            implementation(projects.shared.core)
            implementation(projects.shared.libs.koin)
            implementation(projects.shared.libs.http)
            implementation(projects.shared.libs.arch)
            implementation(projects.shared.libs.coroutinesX)
            implementation(projects.shared.libs.preferences)
            implementation(projects.shared.libs.crashlytics)
            implementation(projects.shared.libs.firebasePerf)

            implementation(libs.androidx.paging.common)
            implementation(libs.androidx.paging.compose)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
            implementation(projects.shared.testSupport)
        }
        androidUnitTest.dependencies {
            implementation(libs.mockk)
        }
    }
}
