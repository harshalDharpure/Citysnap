import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.google.services)
    alias(libs.plugins.firebase.crashlytics)
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.ksp)
}

val keystoreProperties = Properties()
val keystorePropertiesFile = rootProject.file("keystore.properties")
if (keystorePropertiesFile.exists()) {
    keystorePropertiesFile.inputStream().use { keystoreProperties.load(it) }
}

val localProperties = Properties()
val localPropertiesFile = rootProject.file("local.properties")
if (localPropertiesFile.exists()) {
    localPropertiesFile.inputStream().use { localProperties.load(it) }
}

fun signingSecret(name: String): String? =
    (keystoreProperties.getProperty(name) ?: System.getenv(name))?.takeIf { it.isNotBlank() }

android {
    namespace = "com.prod.singles_date"
    compileSdk {
        version = release(36) {
            minorApiLevel = 1
        }
    }

    defaultConfig {
        applicationId = "com.prod.singles_date"
        minSdk = 24
        targetSdk = 36
        versionCode = 26
        versionName = "6.0.5"

        // Fallback if resource shrinking ever strips default_web_client_id from google-services.json.
        buildConfigField(
            "String",
            "GOOGLE_WEB_CLIENT_ID",
            "\"32449532472-lmg727mpcn2mrr78p2qlrbpj1on754t5.apps.googleusercontent.com\"",
        )

        val s3ApiBaseUrl = localProperties.getProperty("S3_API_BASE_URL")?.trim()?.takeIf { it.isNotBlank() }
            ?: "https://q7dxdauc8f.execute-api.us-east-1.amazonaws.com"
        buildConfigField("String", "S3_API_BASE_URL", "\"$s3ApiBaseUrl\"")

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    signingConfigs {
        create("release") {
            val releaseStoreFile = signingSecret("RELEASE_STORE_FILE")
            if (releaseStoreFile != null) {
                storeFile = rootProject.file(releaseStoreFile)
                storePassword = signingSecret("RELEASE_STORE_PASSWORD")
                keyAlias = signingSecret("RELEASE_KEY_ALIAS")
                keyPassword = signingSecret("RELEASE_KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            // Enable R8 so Play Console can use mapping.txt for crash deobfuscation.
            isMinifyEnabled = true
            // Optional but recommended for smaller APK/AAB.
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            signingConfigs.getByName("release").takeIf {
                it.storeFile != null &&
                    it.storePassword != null &&
                    it.keyAlias != null &&
                    it.keyPassword != null
            }?.let { signingConfig = it }

            // Some dependencies ship native libs (e.g., gRPC/Firestore). Generate symbols for Play Console.
            ndk {
                debugSymbolLevel = "FULL"
            }
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
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.activity.compose)
    // Required for registerForActivityResult in MainActivity (lint: InvalidFragmentVersionForActivityResult).
    implementation(libs.androidx.fragment.ktx)
    implementation(libs.androidx.navigation.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation("androidx.compose.material:material-icons-extended")

    // Firebase (Auth + Firestore). Add `google-services.json` under `app/`.
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth)
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.messaging)
    implementation(libs.firebase.analytics)
    implementation(libs.firebase.crashlytics)
    implementation(libs.kotlinx.coroutines.play.services)
    implementation(libs.coil.compose)
    implementation(libs.androidx.compose.foundation)
    implementation(libs.okhttp)

    // Google sign-in via Credential Manager.
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services.auth)
    implementation(libs.googleid)
    implementation(libs.play.services.auth)
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)
    implementation(libs.firebase.appcheck.playintegrity)
    debugImplementation(libs.firebase.appcheck.debug)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}