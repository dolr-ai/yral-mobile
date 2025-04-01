import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
  alias(libs.plugins.kotlinMultiplatform)
  alias(libs.plugins.androidApplication)
  alias(libs.plugins.compose.jetbrains)
  alias(libs.plugins.compose.compiler)
}

kotlin {
  androidTarget {
    compilerOptions {
      jvmTarget.set(JvmTarget.JVM_17)
    }
  }
//  listOf(
//    iosX64(),
//    iosArm64(),
//    iosSimulatorArm64()
//  ).forEach { iosTarget ->
//    iosTarget.binaries.framework {
//      baseName = "ComposeApp"
//      isStatic = true
//    }
//  }
  sourceSets {
    androidMain.dependencies {
      implementation(libs.androidx.activity.compose)
    }
    commonMain.dependencies {
      implementation(compose.runtime)
      implementation(compose.foundation)
      implementation(compose.material3)
      implementation(compose.ui)
      implementation(compose.components.resources)
      implementation(compose.components.uiToolingPreview)

      implementation(projects.shared.core)
      implementation(projects.shared.rust)
    }
  }
}

android {
  namespace = "com.yral.composeApp"
  compileSdk = libs.versions.compileSDK.get().toInt()
  defaultConfig {
    applicationId = "com.yral.android"
    minSdk = libs.versions.minSDK.get().toInt()
    targetSdk = libs.versions.targetSDK.get().toInt()
    versionCode = 1
    versionName = "1.0"
  }
  packaging {
    resources {
      excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }
  }
  buildTypes {
    getByName("release") {
      isMinifyEnabled = false
    }
  }
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
  }
}

dependencies {
  debugImplementation(compose.uiTooling)
}