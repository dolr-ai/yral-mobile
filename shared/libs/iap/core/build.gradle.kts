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
            implementation(projects.shared.libs.coroutinesX)
            implementation(projects.shared.libs.koin)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kotlinx.datetime)
            implementation(libs.touchlab.logger)
        }
        androidMain.dependencies {
            implementation(libs.googlePlay.billingclient)
            implementation(libs.googlePlay.billingclient.ktx)
        }
    }
}
