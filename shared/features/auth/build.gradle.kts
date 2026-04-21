import com.yral.buildlogic.applyCocoapodsIfApple
import com.yral.buildlogic.configureCocoapods
import com.yral.buildlogic.configureIosTargets
import com.yral.buildlogic.isAppleBuildEnabled
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
plugins {
    alias(libs.plugins.yral.shared.feature)
    alias(libs.plugins.yral.android.feature)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.yral.shared.library.compose)
}

applyCocoapodsIfApple()

configureCocoapods {
    version = "1.0"
    summary = "Authentication feature"
    homepage = "https://github.com/dolr-ai/yral-mobile"
    ios.deploymentTarget = "15.6"

    noPodspec()

    pod("FirebaseMessaging") {
        version = "11.14.0"
    }
}

kotlin {
    androidTarget()
    configureIosTargets(project)

    if (project.isAppleBuildEnabled()) {
        fun cocoapodsFrameworks(
            projectPath: String,
            vararg frameworks: Pair<String, String>,
        ): List<Pair<String, String>> {
            val iosBuildPath =
                project(projectPath)
                    .layout
                    .buildDirectory
                    .dir("cocoapods/synthetic/ios/build/Debug-iphonesimulator")
                    .get()
                    .asFile
            return frameworks.map { (relativePath, frameworkName) ->
                iosBuildPath.resolve(relativePath).absolutePath to frameworkName
            }
        }

        val transitiveCocoapodsFrameworks =
            (
                cocoapodsFrameworks(
                    projectPath = ":shared:libs:analytics",
                    "XCFrameworkIntermediates/FBSDKCoreKit" to "FBSDKCoreKit",
                    "XCFrameworkIntermediates/FBAEMKit" to "FBAEMKit",
                    "XCFrameworkIntermediates/FirebaseAnalytics/Default" to "FirebaseAnalytics",
                    "XCFrameworkIntermediates/GoogleAdsOnDeviceConversion" to "GoogleAdsOnDeviceConversion",
                    "XCFrameworkIntermediates/FBSDKCoreKit_Basics" to "FBSDKCoreKit_Basics",
                    "XCFrameworkIntermediates/GoogleAppMeasurement/Core" to "GoogleAppMeasurement",
                    "XCFrameworkIntermediates/GoogleAppMeasurement/IdentitySupport" to "GoogleAppMeasurementIdentitySupport",
                    "FirebaseCore" to "FirebaseCore",
                    "PromisesObjC" to "FBLPromises",
                    "FirebaseInstallations" to "FirebaseInstallations",
                    "Mixpanel" to "Mixpanel",
                    "FirebaseCoreInternal" to "FirebaseCoreInternal",
                    "GoogleUtilities" to "GoogleUtilities",
                    "nanopb" to "nanopb",
                ) + cocoapodsFrameworks(
                    projectPath = ":shared:libs:branch",
                    "BranchSDK" to "BranchSDK",
                ) + cocoapodsFrameworks(
                    projectPath = ":shared:libs:crashlytics",
                    "FirebaseRemoteConfigInterop" to "FirebaseRemoteConfigInterop",
                    "FirebaseCrashlytics" to "FirebaseCrashlytics",
                    "FirebaseCore" to "FirebaseCore",
                    "PromisesObjC" to "FBLPromises",
                    "GoogleDataTransport" to "GoogleDataTransport",
                    "FirebaseCoreExtension" to "FirebaseCoreExtension",
                    "FirebaseInstallations" to "FirebaseInstallations",
                    "FirebaseCoreInternal" to "FirebaseCoreInternal",
                    "FirebaseSessions" to "FirebaseSessions",
                    "GoogleUtilities" to "GoogleUtilities",
                    "PromisesSwift" to "Promises",
                    "nanopb" to "nanopb",
                ) + cocoapodsFrameworks(
                    projectPath = ":shared:libs:feature-flag",
                    "FirebaseRemoteConfigInterop" to "FirebaseRemoteConfigInterop",
                    "FirebaseSharedSwift" to "FirebaseSharedSwift",
                    "FirebaseCore" to "FirebaseCore",
                    "PromisesObjC" to "FBLPromises",
                    "FirebaseABTesting" to "FirebaseABTesting",
                    "FirebaseRemoteConfig" to "FirebaseRemoteConfig",
                    "FirebaseInstallations" to "FirebaseInstallations",
                    "FirebaseCoreInternal" to "FirebaseCoreInternal",
                    "GoogleUtilities" to "GoogleUtilities",
                ) + cocoapodsFrameworks(
                    projectPath = ":shared:libs:firebasePerf",
                    "FirebaseRemoteConfigInterop" to "FirebaseRemoteConfigInterop",
                    "FirebaseSharedSwift" to "FirebaseSharedSwift",
                    "FirebaseCore" to "FirebaseCore",
                    "PromisesObjC" to "FBLPromises",
                    "GoogleDataTransport" to "GoogleDataTransport",
                    "FirebaseABTesting" to "FirebaseABTesting",
                    "FirebaseRemoteConfig" to "FirebaseRemoteConfig",
                    "FirebaseCoreExtension" to "FirebaseCoreExtension",
                    "FirebaseInstallations" to "FirebaseInstallations",
                    "FirebaseCoreInternal" to "FirebaseCoreInternal",
                    "FirebaseSessions" to "FirebaseSessions",
                    "GoogleUtilities" to "GoogleUtilities",
                    "PromisesSwift" to "Promises",
                    "nanopb" to "nanopb",
                    "FirebasePerformance" to "FirebasePerformance",
                ) + cocoapodsFrameworks(
                    projectPath = ":shared:features:auth",
                    "FirebaseRemoteConfigInterop" to "FirebaseRemoteConfigInterop",
                )
            )
                .distinct()

        val transitiveCocoapodsLinkerOptions =
            transitiveCocoapodsFrameworks
                .map { (frameworkPath, _) -> frameworkPath }
                .distinct()
                .flatMap { listOf("-F$it", "-rpath", it) } +
                transitiveCocoapodsFrameworks
                    .map { (_, frameworkName) -> frameworkName }
                    .distinct()
                    .flatMap { listOf("-framework", it) }

        targets
            .withType<KotlinNativeTarget>()
            .configureEach {
                if (name == "iosSimulatorArm64") {
                    binaries.configureEach {
                        linkerOpts(*transitiveCocoapodsLinkerOptions.toTypedArray())
                    }
                }
            }
    }

    sourceSets {
        commonTest.dependencies {
            implementation(libs.kotlinx.coroutines.test)
            implementation(libs.ktor.client.mock)
            implementation(libs.russhwolf.multiplatformSettings.test)
            implementation(projects.shared.testSupport)
        }
        androidMain.dependencies {
            implementation(project.dependencies.platform(libs.firebase.bom))
            implementation(libs.firebase.messaging)
            implementation(libs.nimbus.jose.jwt)
            implementation(libs.androidx.browser)
        }
        commonMain.dependencies {
            implementation(projects.shared.core)
            implementation(projects.shared.data)
            implementation(projects.shared.features.wallet)
            implementation(projects.shared.libs.preferences)
            implementation(projects.shared.libs.http)
            implementation(projects.shared.libs.analytics)
            implementation(projects.shared.libs.crashlytics)
            implementation(projects.shared.libs.koin)
            implementation(projects.shared.libs.arch)
            implementation(projects.shared.libs.coroutinesX)
            implementation(projects.shared.rust.service)
            implementation(projects.shared.libs.designsystem)
            implementation(projects.shared.libs.featureFlag)
            implementation(projects.shared.libs.phoneValidation)
        }
    }
}
