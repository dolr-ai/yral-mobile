import com.yral.buildlogic.applyCocoapodsIfApple
import com.yral.buildlogic.configureCocoapods
import com.yral.buildlogic.configureIosTargets
import com.yral.buildlogic.ifAppleBuild
plugins {
    alias(libs.plugins.yral.shared.library)
    alias(libs.plugins.yral.android.library)
}

applyCocoapodsIfApple()

configureCocoapods {
    version = "1.0"
    summary = "Branch SDK module providing Branch dependencies and cinterop"
    homepage = "https://github.com/dolr-ai/yral-mobile"
    ios.deploymentTarget = "15.6"

    noPodspec()

    pod("BranchSDK") {
        extraOpts += listOf("-compiler-option", "-fmodules")
        version = "3.12.1"
    }
}

kotlin {
    androidTarget()
    configureIosTargets(project)

    sourceSets {
        commonMain {
            dependencies {
                // No common dependencies needed
            }
        }
        androidMain.dependencies {
            // Expose as API so consuming modules can use these dependencies
            api(libs.branch)
            api(libs.play.services.ads.identifier)
        }
    }
}
