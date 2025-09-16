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
        commonMain.dependencies {
            api(projects.shared.libs.routing.routesApi)
            implementation(libs.ktor.http)
            implementation(libs.kotlinx.serialization.properties)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}
