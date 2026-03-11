import com.yral.buildlogic.applyCocoapodsIfApple
import com.yral.buildlogic.configureCocoapods
import com.yral.buildlogic.configureIosTargets
import com.yral.buildlogic.ifAppleBuild
plugins {
    alias(libs.plugins.yral.shared.library)
    alias(libs.plugins.yral.android.library)
    alias(libs.plugins.kotlin.serialization)
}

applyCocoapodsIfApple()

configureCocoapods {
    version = "1.0"
    summary = "Firestore"
    homepage = "https://github.com/dolr-ai/yral-mobile"
    ios.deploymentTarget = "15.6"

    noPodspec()

    pod("FirebaseAppCheck") {
        version = "11.14.0"
    }
}

kotlin {
    androidTarget()
    configureIosTargets(project)

    sourceSets {
        commonMain.dependencies {
            implementation(projects.shared.libs.koin)
            implementation(projects.shared.core)
            implementation(projects.shared.libs.arch)
            implementation(projects.shared.libs.coroutinesX)

            api(libs.gitlive.firebase.storage)
            api(libs.gitlive.firebase.store.db)
            implementation(libs.ktor.serialization.kotlinx.json)
        }
    }
}
