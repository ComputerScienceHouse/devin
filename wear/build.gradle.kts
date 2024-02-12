import org.jetbrains.kotlin.konan.properties.Properties

plugins {
  id("com.android.application")
  id("org.jetbrains.kotlin.android")
  kotlin("kapt")
  id("com.google.dagger.hilt.android")
}

android {
  namespace = "edu.rit.csh.devin"
  compileSdk = 34

  defaultConfig {
    applicationId = "edu.rit.csh.devin"
    minSdk = 26
    targetSdk = 33
    val properties = Properties().apply {
      load(File("version.properties").reader())
    }
    versionCode = Integer.parseInt(properties.getProperty("versionCode"))
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
  kotlinOptions {
    jvmTarget = "17"
  }
  buildFeatures {
    compose = true
  }
  composeOptions {
    kotlinCompilerExtensionVersion = "1.4.3"
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
  implementation("com.google.android.gms:play-services-wearable:18.0.0")
  implementation(platform("androidx.compose:compose-bom:2023.08.00"))
  implementation("androidx.compose.ui:ui")
  implementation("androidx.compose.ui:ui-tooling-preview")
  implementation("androidx.wear.compose:compose-material:1.1.2")
  implementation("androidx.wear.compose:compose-foundation:1.1.2")
  implementation("androidx.activity:activity-compose:1.7.2")
  implementation("androidx.core:core-splashscreen:1.0.1")
  implementation("androidx.wear:wear-phone-interactions:1.0.1")
  androidTestImplementation(platform("androidx.compose:compose-bom:2023.08.00"))
  androidTestImplementation("androidx.compose.ui:ui-test-junit4")
  debugImplementation("androidx.compose.ui:ui-tooling")
  debugImplementation("androidx.compose.ui:ui-test-manifest")

  implementation(platform("com.okta.kotlin:bom:1.2.0"))

  implementation("com.okta.kotlin:auth-foundation")
  implementation("com.okta.kotlin:auth-foundation-bootstrap")
  implementation("com.okta.kotlin:oauth2")

  implementation("com.google.dagger:hilt-android:2.48")
  kapt("com.google.dagger:hilt-android-compiler:2.44")
  implementation("androidx.hilt:hilt-navigation-compose:1.1.0")
  implementation("androidx.compose.material:material-icons-extended")
  implementation("com.squareup.okhttp3:okhttp:4.11.0")
  implementation("com.squareup.retrofit2:retrofit:2.9.0")
  implementation("com.squareup.retrofit2:converter-gson:2.9.0")
  implementation("com.squareup.okhttp3:logging-interceptor:4.11.0")

  implementation("androidx.security:security-crypto-ktx:1.1.0-alpha06")
  implementation("com.google.android.horologist:horologist-compose-layout:0.5.17")
}

kapt {
  correctErrorTypes = true
}