plugins {
  id("com.android.library")
  id("org.jetbrains.kotlin.android")
  kotlin("kapt")
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
      isMinifyEnabled = false
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
    }
  }
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
  }
  kotlinOptions {
    jvmTarget = "17"
  }
}

dependencies {

  implementation("androidx.core:core-ktx:1.12.0")
  implementation("androidx.appcompat:appcompat:1.6.1")
  implementation("com.google.android.material:material:1.9.0")
  implementation(platform("androidx.compose:compose-bom:2023.08.00"))
  implementation("androidx.compose.material:material-icons-extended")
  testImplementation("junit:junit:4.13.2")
  androidTestImplementation("androidx.test.ext:junit:1.1.5")
  androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")

  // Ensure all dependencies are compatible using the Bill of Materials (BOM).
  implementation(platform("com.okta.kotlin:bom:1.2.0"))

  // Add the dependencies to your project.
  implementation("com.okta.kotlin:auth-foundation")
  implementation("com.okta.kotlin:auth-foundation-bootstrap")
  implementation("com.okta.kotlin:oauth2")

  implementation("com.google.dagger:hilt-android:2.48")
  kapt("com.google.dagger:hilt-android-compiler:2.44")
  implementation("androidx.hilt:hilt-navigation-compose:1.1.0")

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
