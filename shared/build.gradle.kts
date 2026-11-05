plugins {
  id("com.android.library")
  id("com.android.legacy-kapt")
  id("com.google.dagger.hilt.android")
}

android {
  namespace = "edu.rit.csh.devin.shared"
  compileSdk = 34

  defaultConfig {
    minSdk = 24

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    consumerProguardFiles("consumer-rules.pro")
  }

  buildTypes {
    release {
      isMinifyEnabled = true
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
    }
  }
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
  }

  packaging {
    resources {
      excludes += "/org/bouncycastle/**"
    }
  }
}

dependencies {
  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.appcompat)
  implementation(libs.material)
  implementation(platform(libs.androidx.compose.bom))
  implementation(libs.material.icons.extended)
  testImplementation(libs.junit)
  androidTestImplementation(libs.androidx.junit)
  androidTestImplementation(libs.androidx.espresso.core)

  // Ensure all dependencies are compatible using the Bill of Materials (BOM).
  implementation(platform(libs.bom))

  // Add the dependencies to your project.
  implementation(libs.okta.auth.foundation)
  implementation(libs.okta.auth.foundation.bootstrap)
  implementation(libs.okta.oauth2)

  implementation(libs.hilt.android)
  kapt(libs.hilt.android.compiler)
  implementation(libs.androidx.hilt.navigation.compose)

  implementation(libs.okhttp)
  implementation(libs.retrofit)
  implementation(libs.converter.gson)
  implementation(libs.logging.interceptor)

  implementation(libs.bcpkix.jdk15to18)
  implementation(libs.bcprov.jdk15to18)
  implementation(libs.androidx.security.crypto.ktx)
}

kapt {
  correctErrorTypes = true
}
