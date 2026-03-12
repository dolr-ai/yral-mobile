import com.yral.buildlogic.configureIosTargets
plugins {
    alias(libs.plugins.yral.shared.library)
    alias(libs.plugins.yral.android.library)
    alias(libs.plugins.yral.shared.library.compose)
}

kotlin {
    androidTarget()
    configureIosTargets(project)

    sourceSets {
        commonMain.dependencies {
            implementation(compose.components.resources)
            implementation(projects.shared.libs.designsystem)
            implementation(projects.shared.libs.formatters)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }
}

dependencies {
    debugImplementation(compose.uiTooling)
}

compose.resources {
    publicResClass = true
}
