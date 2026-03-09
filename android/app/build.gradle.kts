plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp")
}

android {
    namespace = "com.carlos.uberanalyzer"
    compileSdk = 35

    defaultConfig {
        applicationId = "com.carlos.uberanalyzer"
        minSdk = 26
        targetSdk = 35
        versionCode = (project.findProperty("versionCode") as String?)?.toInt() ?: 3
        versionName = (project.findProperty("versionName") as String?) ?: "1.0.0"

        // Ads disabled by default (testing). Set -PSHOW_ADS=true for production builds.
        buildConfigField("boolean", "SHOW_ADS",
            (project.findProperty("SHOW_ADS") as String?) ?: "false")
    }

    buildFeatures {
        buildConfig = true
    }

    signingConfigs {
        create("release") {
            val ksFile = file(project.findProperty("KEYSTORE_FILE") as String? ?: "../release.keystore")
            if (ksFile.exists()) {
                storeFile = ksFile
                storePassword = (project.findProperty("KEYSTORE_PASSWORD") as String?) ?: "panauber2026"
                keyAlias = (project.findProperty("KEY_ALIAS") as String?) ?: "panauber"
                keyPassword = (project.findProperty("KEY_PASSWORD") as String?) ?: "panauber2026"
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            signingConfig = signingConfigs.getByName("release")
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
    implementation("com.google.android.material:material:1.11.0")
    implementation("androidx.constraintlayout:constraintlayout:2.1.4")
    implementation("androidx.recyclerview:recyclerview:1.3.2")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")

    // Room
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")

    // ML Kit Text Recognition (on-device OCR)
    implementation("com.google.mlkit:text-recognition:16.0.1")

    // PostHog Analytics
    implementation("com.posthog:posthog-android:3.+")

    // AdMob
    implementation("com.google.android.gms:play-services-ads:23.6.0")

    // Google Play Billing
    implementation("com.android.billingclient:billing-ktx:7.1.1")
}
