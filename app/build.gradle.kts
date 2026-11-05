import org.jetbrains.kotlin.konan.properties.Properties

plugins {
  id("com.android.application")
  id("com.android.legacy-kapt")
  id("com.google.dagger.hilt.android")
  id("org.jetbrains.kotlin.plugin.compose")
}

android {
  namespace = "edu.rit.csh.devin"
  compileSdk = 34

  defaultConfig {
    applicationId = "edu.rit.csh.devin"
    minSdk = 24
    targetSdk = 34
    val properties = Properties().apply {
      load(File("version.properties").reader())
    }
    versionCode = (Integer.parseInt(properties.getProperty("versionCode")) shl 2) or 0
    versionName = properties.getProperty("versionName")

    manifestPlaceholders["webAuthenticationRedirectScheme"] = "edu.rit.csh.devin"

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    vectorDrawables {
      useSupportLibrary = true
    }
  }

  signingConfigs {
    create("release") {
      val properties = Properties().apply {
        load(File("key.properties").reader())
      }
      keyAlias = properties.getProperty("keyAlias")
      keyPassword = properties.getProperty("keyPassword")
      storeFile = File(properties.getProperty("storeFile"))
      storePassword = properties.getProperty("storePassword")
    }
  }

  buildTypes {
    release {
      isMinifyEnabled = true
      isShrinkResources = true
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
      isDebuggable = false
      signingConfig = signingConfigs.getByName("release")
    }
//    debug {
//      isMinifyEnabled = true
//      isShrinkResources = true
//      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
//      isDebuggable = true
//    }
  }
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
  }
  buildFeatures {
    compose = true
  }
  packaging {
    resources {
      excludes += "/META-INF/{AL2.0,LGPL2.1}"
      excludes += "/org/bouncycastle/**"
    }
  }
}

dependencies {
  implementation(project(":shared"))
  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.activity.compose)
  implementation(platform(libs.androidx.compose.bom))
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.graphics)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.compose.material3)
  testImplementation(libs.junit)
  androidTestImplementation(libs.androidx.junit)
  androidTestImplementation(libs.androidx.espresso.core)
  androidTestImplementation(platform(libs.androidx.compose.bom))
  androidTestImplementation(libs.androidx.compose.ui.test.junit4)
  debugImplementation(libs.androidx.compose.ui.tooling)
  debugImplementation(libs.androidx.compose.ui.test.manifest)

  implementation(libs.okhttp)
  implementation(libs.retrofit)
  implementation(libs.converter.gson)
  implementation(libs.logging.interceptor)

  // Ensure all dependencies are compatible using the Bill of Materials (BOM).
  implementation(platform(libs.bom))

  // Add the dependencies to your project.
  implementation(libs.okta.auth.foundation)
  implementation(libs.okta.auth.foundation.bootstrap)
  implementation(libs.oauth2)
  implementation(libs.web.authentication.ui)

  implementation(libs.hilt.android)
  kapt(libs.hilt.android.compiler)
  implementation(libs.androidx.hilt.navigation.compose)
  implementation(libs.androidx.compose.material.icons.extended)
}

kapt {
  correctErrorTypes = true
}
