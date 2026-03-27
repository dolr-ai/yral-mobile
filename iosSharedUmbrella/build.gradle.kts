import com.yral.buildlogic.applyCocoapodsIfApple
import com.yral.buildlogic.configureCocoapods
import com.yral.buildlogic.configureIosTargets
plugins {
    alias(libs.plugins.yral.shared.feature)
    alias(libs.plugins.yral.shared.library.compose)
    alias(libs.plugins.kotlin.serialization)
}

applyCocoapodsIfApple()

configureCocoapods {
    summary = "Umbrella framework for shared KMM code"
    homepage = "https://github.com/dolr-ai/yral-mobile"
    license = "MIT"
    ios.deploymentTarget = "15.6"
    podfile = project.file("../iosApp/Podfile")

    framework {
        baseName = "iosSharedUmbrella"
        isStatic = true
        export(projects.shared.core)
        export(projects.shared.libs.analytics)
        export(projects.shared.libs.crashlytics)
        export(projects.shared.app)
        export(projects.shared.libs.featureFlag)
        export(projects.shared.libs.routing.routesApi)
        export(libs.decompose.decompose)
        export(libs.essenty.lifecycle)
    }
}

version = "1.0"
kotlin {
    configureIosTargets(project)

    sourceSets {
        iosMain.dependencies {
            api(projects.shared.core)
            api(projects.shared.app)
            api(projects.shared.libs.analytics)
            api(projects.shared.libs.crashlytics)
            api(projects.shared.libs.featureFlag)
            api(projects.shared.libs.routing.routesApi)
            implementation(compose.components.resources)
        }
    }
}
