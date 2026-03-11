import com.yral.buildlogic.applyCocoapodsIfApple
import com.yral.buildlogic.configureIosTargets
import com.yral.buildlogic.ifAppleBuild
plugins {
    alias(libs.plugins.yral.shared.library)
    alias(libs.plugins.yral.android.library)
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    androidTarget()
    configureIosTargets(project)

    sourceSets {
        commonMain.dependencies {
            implementation(projects.shared.libs.koin)

            api(libs.gitlive.firebase.kotlin.crashlytics)
            implementation(libs.sentry.kmp)
            implementation(libs.touchlab.logger)
        }
    }
}
