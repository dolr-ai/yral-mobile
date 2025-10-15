plugins {
    alias(libs.plugins.yral.shared.library)
    alias(libs.plugins.yral.android.library)
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    androidTarget()
    listOf(
        iosArm64(),
        iosSimulatorArm64(),
    )

    sourceSets {
        androidMain.dependencies {
            api(libs.ktor.client.okhttp)
            implementation(project.dependencies.platform(libs.okhttp.bom))
            implementation(libs.okhttp.dns)
        }
        commonMain.dependencies {
            api(libs.ktor.client.core)
            api(libs.ktor.client.logging)
            api(libs.ktor.client.content.negotiation)
            api(libs.ktor.serialization.kotlinx.json)
            api(libs.kotlinx.datetime)

            implementation(projects.shared.libs.preferences)
            implementation(projects.shared.libs.koin)
            implementation(projects.shared.core)
        }
        iosMain.dependencies {
            implementation(libs.ktor.client.darwin)
        }
    }
}

dependencies {
    debugImplementation(libs.chucker.debug)
    releaseImplementation(libs.chucker.release)
}
