import java.io.File
import java.util.Properties

plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.compose)
  alias(libs.plugins.secrets)
}

fun resolveProjectSecret(keyName: String, projectRootDir: File): String {
  val envVal = System.getenv(keyName)?.trim().orEmpty()
  if (envVal.isNotEmpty()) return envVal

  for (fileName in listOf(".env", "local.properties", "secrets.properties")) {
    val f = File(projectRootDir, fileName)
    if (f.exists()) {
      try {
        val props = Properties()
        val stream = f.inputStream()
        try {
          props.load(stream)
        } finally {
          stream.close()
        }
        val v = props.getProperty(keyName)?.trim()?.removeSurrounding("\"")?.removeSurrounding("'").orEmpty()
        if (v.isNotEmpty()) return v
      } catch (_: Exception) {
      }
    }
  }

  val devEnvFile = File("/app/.dev.env.json")
  if (devEnvFile.exists()) {
    try {
      val content = devEnvFile.readText()
      val regex = Regex("\"${Regex.escape(keyName)}\"\\s*:\\s*\"([^\"]+)\"")
      val match = regex.find(content)?.groupValues?.getOrNull(1)?.trim().orEmpty()
      if (match.isNotEmpty()) return match
    } catch (_: Exception) {
    }
  }

  return ""
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

    val geminiKey = resolveProjectSecret("GEMINI_API_KEY", rootDir).replace("\\", "\\\\").replace("\"", "\\\"")
    buildConfigField("String", "GEMINI_API_KEY", "\"$geminiKey\"")

    val groqKey = resolveProjectSecret("GROQ_API_KEY", rootDir).replace("\\", "\\\\").replace("\"", "\\\"")
    buildConfigField("String", "GROQ_API_KEY", "\"$groqKey\"")

    val longcatKey = resolveProjectSecret("LONGCAT_API_KEY", rootDir).replace("\\", "\\\\").replace("\"", "\\\"")
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
      signingConfig = signingConfigs.getByName("debug")
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
  implementation(libs.androidx.datastore.preferences)
  implementation(libs.kotlinx.coroutines.android)
  implementation(libs.okhttp)
  testImplementation(libs.junit)
  testImplementation(libs.androidx.junit)
  testImplementation(libs.androidx.core)
  testImplementation(libs.androidx.compose.ui.test.junit4)
  debugImplementation(libs.androidx.compose.ui.test.manifest)
  debugImplementation(libs.androidx.compose.ui.tooling)
}
