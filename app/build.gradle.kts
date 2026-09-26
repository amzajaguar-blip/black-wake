import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

val keystoreProperties = Properties()
val keystorePropertiesFile = rootProject.file("keystore.properties")
if (keystorePropertiesFile.exists()) {
    keystorePropertiesFile.inputStream().use(keystoreProperties::load)
}

fun signingValue(environmentName: String, propertyName: String): String? =
    System.getenv(environmentName)?.takeIf { it.isNotBlank() }
        ?: keystoreProperties.getProperty(propertyName)?.takeIf { it.isNotBlank() }

val uploadStoreFile = signingValue("BW_UPLOAD_STORE_FILE", "storeFile")
val uploadStorePassword = signingValue("BW_UPLOAD_STORE_PASSWORD", "storePassword")
val uploadKeyAlias = signingValue("BW_UPLOAD_KEY_ALIAS", "keyAlias")
val uploadKeyPassword = signingValue("BW_UPLOAD_KEY_PASSWORD", "keyPassword")
val uploadSigningConfigured = listOf(
    uploadStoreFile, uploadStorePassword, uploadKeyAlias, uploadKeyPassword
).any { it != null }

if (uploadSigningConfigured) {
    val missing = mutableListOf<String>()
    if (uploadStoreFile == null) missing += "BW_UPLOAD_STORE_FILE (storeFile)"
    if (uploadStorePassword == null) missing += "BW_UPLOAD_STORE_PASSWORD (storePassword)"
    if (uploadKeyAlias == null) missing += "BW_UPLOAD_KEY_ALIAS (keyAlias)"
    if (uploadKeyPassword == null) missing += "BW_UPLOAD_KEY_PASSWORD (keyPassword)"
    if (uploadStoreFile != null && !rootProject.file(uploadStoreFile).isFile) {
        missing += "BW_UPLOAD_STORE_FILE (file not found)"
    }
    if (missing.isNotEmpty()) {
        throw GradleException("Release signing partially configured: missing ${missing.joinToString(", ")}")
    }
}

android {
    namespace = "com.blackwake.game"
    compileSdk = 36

    defaultConfig {
        // The Play Console identity. Deliberately different from the namespace above:
        // the namespace only names the Kotlin/R/BuildConfig package and renaming it
        // would touch every source file for no gain, since Play only sees this.
        applicationId = "com.frenzy_rush"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0.0"

        vectorDrawables {
            useSupportLibrary = true
        }
    }

    if (uploadSigningConfigured) {
        signingConfigs {
            create("upload") {
                storeFile = rootProject.file(uploadStoreFile!!)
                storePassword = uploadStorePassword!!
                keyAlias = uploadKeyAlias!!
                keyPassword = uploadKeyPassword!!
            }
        }
        logger.lifecycle("Release signing: upload key configured")
    } else {
        logger.lifecycle("Release signing: not configured, building unsigned")
    }

    buildTypes {
        release {
            if (uploadSigningConfigured) {
                signingConfig = signingConfigs.getByName("upload")
            }
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    testOptions {
        unitTests.isReturnDefaultValues = true
    }
    lint {
        abortOnError = false
        checkReleaseBuilds = false
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_17)
    }
}

// Held at the last versions compatible with AGP 8.x: AndroidX releases from late 2026
// require AGP 9.1+, which is a separate platform upgrade (Gradle 9, built-in Kotlin plugin).
dependencies {
    implementation(platform("androidx.compose:compose-bom:2025.06.00"))
    implementation("androidx.core:core-ktx:1.16.0")
    implementation("androidx.activity:activity-compose:1.10.1")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.9.2")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.9.2")
    implementation("androidx.lifecycle:lifecycle-runtime-compose:2.9.2")
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.10.2")
    debugImplementation("androidx.compose.ui:ui-tooling")

    testImplementation("junit:junit:4.13.2")
}
