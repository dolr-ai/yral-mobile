import com.yral.buildlogic.configureIosTargets
plugins {
    alias(libs.plugins.yral.shared.library)
    alias(libs.plugins.yral.android.library)
}

kotlin {
    androidTarget()
    configureIosTargets(project)

    sourceSets {
        commonMain {
            dependencies {
                implementation(projects.shared.libs.coroutinesX)
                implementation(projects.shared.libs.branch)
            }
        }
        androidMain.dependencies {
            api(libs.coil.core)
        }
    }
}
