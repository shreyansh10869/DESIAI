plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.compose)
  alias(libs.plugins.secrets)
}

android {
  namespace = "com.example"
  compileSdk { version = release(36) { minorApiLevel = 1 } }

  defaultConfig {
    applicationId = "com.desiai.official.app"
    minSdk = 24
    targetSdk = 36
    versionCode = 1
    versionName = "2.0-Auralis"

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

    val geminiKey = (System.getenv("GEMINI_API_KEY") ?: "").replace("\"", "\\\"")
    buildConfigField("String", "GEMINI_API_KEY", "\"$geminiKey\"")

    val groqKey = (System.getenv("GROQ_API_KEY") ?: "").replace("\"", "\\\"")
    buildConfigField("String", "GROQ_API_KEY", "\"$groqKey\"")

    val longcatKey = (System.getenv("LONGCAT_API_KEY") ?: "").replace("\"", "\\\"")
    buildConfigField("String", "LONGCAT_API_KEY", "\"$longcatKey\"")
  }

  signingConfigs {
    create("release") {
      val keystorePath = System.getenv("KEYSTORE_PATH") ?: "${rootDir}/my-upload-key.jks"
      storeFile = file(keystorePath)
      storePassword = System.getenv("STORE_PASSWORD")
      keyAlias = "upload"
      keyPassword = System.getenv("KEY_PASSWORD")
    }
  }

  buildTypes {
    release {
      isCrunchPngs = false
      isMinifyEnabled = false
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
      val releaseKeystore = file(System.getenv("KEYSTORE_PATH") ?: "${rootDir}/my-upload-key.jks")
      if (releaseKeystore.exists()) {
        signingConfig = signingConfigs.findByName("release")
      }
    }
    debug {
      // Uses standard Android Gradle Plugin default debug signing config for reliable CI/CD builds
    }
  }
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
  }
  buildFeatures {
    compose = true
    buildConfig = true
  }
  testOptions { unitTests { isIncludeAndroidResources = true } }
  dependenciesInfo {
    includeInApk = false
    includeInBundle = true
  }
}

// Configure the Secrets Gradle Plugin to use .env and .env.example files
// to match the convention used in Web projects.
secrets {
  propertiesFileName = if (rootProject.file(".env").exists()) ".env" else "secrets.properties"
  defaultPropertiesFileName = if (rootProject.file(".env.example").exists()) ".env.example" else "local.defaults.properties"
  ignoreList.add("FIREBASE_APPCHECK_DEBUG_TOKEN")
  ignoreList.add("GEMINI_API_KEY")
  ignoreList.add("GROQ_API_KEY")
  ignoreList.add("LONGCAT_API_KEY")
}

dependencies {
  implementation(platform(libs.androidx.compose.bom))
  implementation(libs.androidx.activity.compose)
  implementation(libs.androidx.compose.material.icons.core)
  implementation(libs.androidx.compose.material.icons.extended)
  implementation(libs.androidx.compose.material3)
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.graphics)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.lifecycle.runtime.compose)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.lifecycle.viewmodel.compose)
  implementation(libs.coil.compose)
  implementation(libs.kotlinx.coroutines.android)
  testImplementation(libs.junit)
  testImplementation(libs.androidx.junit)
  testImplementation(libs.androidx.core)
  testImplementation(libs.androidx.compose.ui.test.junit4)
  debugImplementation(libs.androidx.compose.ui.test.manifest)
  debugImplementation(libs.androidx.compose.ui.tooling)
}
