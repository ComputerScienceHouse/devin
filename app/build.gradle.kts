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
    minSdk = 24
    targetSdk = 34
    versionCode = 137
    versionName = "2.0.0"

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
    debug {
      isMinifyEnabled = true
      isShrinkResources = true
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
      isDebuggable = true
    }
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
    }
  }
}

dependencies {

  implementation("androidx.core:core-ktx:1.9.0")
  implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.6.1")
  implementation("androidx.activity:activity-compose:1.7.0")
  implementation(platform("androidx.compose:compose-bom:2023.03.00"))
  implementation("androidx.compose.ui:ui")
  implementation("androidx.compose.ui:ui-graphics")
  implementation("androidx.compose.ui:ui-tooling-preview")
  implementation("androidx.compose.material3:material3:1.2.0-alpha12")
  testImplementation("junit:junit:4.13.2")
  androidTestImplementation("androidx.test.ext:junit:1.1.5")
  androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
  androidTestImplementation(platform("androidx.compose:compose-bom:2023.03.00"))
  androidTestImplementation("androidx.compose.ui:ui-test-junit4")
  debugImplementation("androidx.compose.ui:ui-tooling")
  debugImplementation("androidx.compose.ui:ui-test-manifest")

  // Ensure all dependencies are compatible using the Bill of Materials (BOM).
  implementation(platform("com.okta.kotlin:bom:1.2.0"))

  // Add the dependencies to your project.
  implementation("com.okta.kotlin:auth-foundation")
  implementation("com.okta.kotlin:auth-foundation-bootstrap")
  implementation("com.okta.kotlin:oauth2")
  implementation("com.okta.kotlin:web-authentication-ui")

  implementation("com.google.dagger:hilt-android:2.48")
  kapt("com.google.dagger:hilt-android-compiler:2.44")
  implementation("androidx.hilt:hilt-navigation-compose:1.1.0")
  implementation("androidx.compose.material:material-icons-extended")
  implementation("com.squareup.okhttp3:okhttp:4.11.0")
  implementation("com.squareup.retrofit2:retrofit:2.9.0")
  implementation("com.squareup.retrofit2:converter-gson:2.9.0")
  implementation("com.squareup.okhttp3:logging-interceptor:4.11.0")

  implementation("com.madgag.spongycastle:core:1.58.0.0")
  implementation("com.madgag.spongycastle:prov:1.58.0.0")
  implementation("androidx.security:security-crypto-ktx:1.1.0-alpha06")
}

kapt {
  correctErrorTypes = true
}
