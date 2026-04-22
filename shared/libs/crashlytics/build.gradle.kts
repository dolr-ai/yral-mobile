import com.yral.buildlogic.applyCocoapodsIfApple
import com.yral.buildlogic.configureCocoapods
import com.yral.buildlogic.configureIosTargets
plugins {
    alias(libs.plugins.yral.shared.library)
    alias(libs.plugins.yral.android.library)
    alias(libs.plugins.kotlin.serialization)
}

applyCocoapodsIfApple()

val firebaseIosSdkVersion =
    libs
        .versions
        .firebase
        .ios
        .sdk
        .get()

configureCocoapods {
    version = "1.0"
    summary = "Shared Crashlytics"
    homepage = "https://github.com/dolr-ai/yral-mobile"
    ios.deploymentTarget = "15.6"

    noPodspec()

    pod("FirebaseCrashlytics") {
        version = firebaseIosSdkVersion
    }
}

kotlin {
    androidTarget()
    configureIosTargets(project)

    sourceSets {
        commonMain.dependencies {
            implementation(projects.shared.libs.koin)
            implementation(libs.touchlab.logger)
        }
        androidMain.dependencies {
            implementation(project.dependencies.platform(libs.firebase.bom))
            api(libs.firebase.crashlytics)
        }
    }
}
