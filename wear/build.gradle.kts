import org.jetbrains.kotlin.konan.properties.Properties

plugins {
  id("com.android.application")
  id("com.android.legacy-kapt")
  id("com.google.dagger.hilt.android")
  id("org.jetbrains.kotlin.plugin.compose")
}

android {
  namespace = "edu.rit.csh.devin"
  compileSdk = 37

  defaultConfig {
    applicationId = "edu.rit.csh.devin"
    minSdk = 26
    targetSdk = 37
    val properties = Properties().apply {
      load(File("version.properties").reader())
    }
    versionCode = (Integer.parseInt(properties.getProperty("versionCode")) shl 2) or 1
    versionName = properties.getProperty("versionName")

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

      proguardFiles(
        getDefaultProguardFile("proguard-android-optimize.txt"),
        "proguard-rules.pro"
      )

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
  implementation(libs.play.services.wearable)
  implementation(platform(libs.androidx.compose.bom))
  implementation(libs.androidx.ui)
  implementation(libs.androidx.ui.tooling.preview)
  implementation(libs.androidx.compose.material)
  implementation(libs.androidx.compose.foundation)
  implementation(libs.androidx.activity.compose)
  implementation(libs.androidx.core.splashscreen)
  implementation(libs.androidx.wear.phone.interactions)
  androidTestImplementation(platform(libs.androidx.compose.bom))
  androidTestImplementation(libs.androidx.ui.test.junit4)
  debugImplementation(libs.androidx.ui.tooling)
  debugImplementation(libs.androidx.ui.test.manifest)

  implementation(platform(libs.bom))

  implementation(libs.okta.auth.foundation)
  implementation(libs.okta.auth.foundation.bootstrap)
  implementation(libs.kotlin.oauth2)

  implementation(libs.hilt.android)
  kapt(libs.hilt.android.compiler)
  implementation(libs.androidx.hilt.navigation.compose)
  implementation(libs.androidx.material.icons.extended)
  implementation(libs.okhttp)
  implementation(libs.retrofit)
  implementation(libs.converter.gson)
  implementation(libs.logging.interceptor)

  implementation(libs.androidx.security.crypto.ktx)
  implementation(libs.horologist.compose.layout)
}

kapt {
  correctErrorTypes = true
}
