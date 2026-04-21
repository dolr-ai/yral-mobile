import com.yral.buildlogic.applyCocoapodsIfApple
import com.yral.buildlogic.configureCocoapods
import com.yral.buildlogic.configureIosTargets
plugins {
    alias(libs.plugins.yral.shared.library)
    alias(libs.plugins.yral.android.library)
}

applyCocoapodsIfApple()

configureCocoapods {
    version = "1.0"
    summary = "Firebase Perf"
    homepage = "https://github.com/dolr-ai/yral-mobile"
    ios.deploymentTarget = "15.6"

    noPodspec()

    pod("FirebasePerformance") {
        version = "11.14.0"
    }
}

kotlin {
    androidTarget()
    configureIosTargets(project)

    sourceSets {
        commonMain.dependencies {
            implementation(projects.shared.core)
        }
        androidMain.dependencies {
            implementation(project.dependencies.platform(libs.firebase.bom))
            implementation(libs.firebase.performance)
        }
    }
}
