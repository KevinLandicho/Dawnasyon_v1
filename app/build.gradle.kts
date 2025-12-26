plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.kotlin.android)

    // ✅ CORRECT: Add this single line here (no extra brackets)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.example.dawnasyon_v1"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.dawnasyon_v1"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        viewBinding = true
    }

    kotlinOptions {
        jvmTarget = "17"
    }
}

dependencies {
    implementation("com.github.PhilJay:MPAndroidChart:v3.1.0")

    // Google ML Kit
    implementation("com.google.android.gms:play-services-mlkit-text-recognition:19.0.0")
    implementation("com.google.android.gms:play-services-mlkit-document-scanner:16.0.0-beta1")
    implementation("org.slf4j:slf4j-simple:2.0.9")
    implementation("io.github.jan-tennert.supabase:storage-kt")

    // Serialization JSON Library
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")

    // Camera & Maps
    implementation("androidx.camera:camera-core:1.2.3")
    implementation("androidx.camera:camera-lifecycle:1.2.3")
    implementation("androidx.camera:camera-view:1.2.3")
    implementation("com.google.android.gms:play-services-maps:18.2.0")
    implementation("org.osmdroid:osmdroid-android:6.1.16")

    // Networking
    implementation("com.squareup.okhttp3:okhttp:4.9.3")

    // Standard Android Libs
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.navigation.fragment)
    implementation(libs.navigation.ui)
    implementation("androidx.security:security-crypto:1.0.0")

    implementation("androidx.core:core-ktx:1.13.1")

    // Testing
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)

    // ⭐ SUPABASE (Keep using the BOM for version management)
    implementation(platform("io.github.jan-tennert.supabase:bom:3.0.0"))
    implementation("io.github.jan-tennert.supabase:postgrest-kt")
    implementation("io.github.jan-tennert.supabase:auth-kt")
    implementation("io.github.jan-tennert.supabase:realtime-kt")

    // ⭐ KTOR ENGINE (Optimized for Android)
    implementation("io.ktor:ktor-client-okhttp:3.0.1")
}