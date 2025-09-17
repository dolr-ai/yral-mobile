plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.kotlinCocoapods)
}

version = "1.0"
kotlin {
    iosArm64()
    iosSimulatorArm64()

    cocoapods {
        summary = "Umbrella framework for shared KMM code"
        homepage = "https://github.com/dolr-ai/yral-mobile"
        license = "MIT"
        ios.deploymentTarget = "15.6"
        podfile = project.file("../iosApp/Podfile")

        framework {
            baseName = "iosSharedUmbrella"
            isStatic = true
            export(projects.shared.libs.analytics)
            export(projects.shared.libs.crashlytics)
            export(projects.shared.app)
            export(projects.shared.libs.featureFlag)
            export(projects.shared.libs.routing.routesApi)
        }
    }

    sourceSets {
        iosMain.dependencies {
            api(projects.shared.app)
            api(projects.shared.libs.analytics)
            api(projects.shared.libs.crashlytics)
            api(projects.shared.libs.featureFlag)
            api(projects.shared.libs.routing.routesApi)
        }
    }
}
