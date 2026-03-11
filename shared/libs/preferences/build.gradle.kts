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
        androidMain.dependencies {
            api(libs.androidx.security.crypto)
            implementation(libs.androidx.datastore.preferences)
        }
        commonMain.dependencies {
            api(libs.russhwolf.multiplatformSettings.core)
            api(libs.russhwolf.multiplatformSettings.coroutines)
            implementation(libs.ktor.serialization.kotlinx.json)
            implementation(libs.kotlinx.serialization.json)

            implementation(projects.shared.core)
            implementation(projects.shared.libs.coroutinesX)
            implementation(projects.shared.libs.koin)
        }
        androidMain.dependencies {
            implementation(libs.russhwolf.multiplatformSettings.datastore)
        }
        iosMain.dependencies {
            implementation(libs.russhwolf.multiplatformSettings.datastore)
            implementation(libs.androidx.datastore)
        }
    }
}
