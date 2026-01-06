plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.jetbrains.kotlin.android)
    alias(libs.plugins.kotlin.serialization)
    id("com.google.gms.google-services")
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
        freeCompilerArgs += "-Xjvm-default=all"
    }
}

dependencies {
    // --- Charting ---
    implementation("com.github.PhilJay:MPAndroidChart:v3.1.0")

    // --- Google ML Kit ---
    implementation("com.google.android.gms:play-services-mlkit-text-recognition:19.0.0")
    implementation("com.google.android.gms:play-services-mlkit-document-scanner:16.0.0-beta1")

    // --- QR Code Scanning ---
    implementation("com.journeyapps:zxing-android-embedded:4.3.0")
    implementation("com.google.zxing:core:3.4.1")

    // --- JSON Serialization & Logging ---
    // ✅ Keep 1.6.3 to match Kotlin 1.9.24
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.3")
    implementation("org.slf4j:slf4j-simple:2.0.9")
    implementation ("org.jsoup:jsoup:1.17.2")

    // --- Camera & Maps ---
    implementation("androidx.camera:camera-core:1.2.3")
    implementation("androidx.camera:camera-lifecycle:1.2.3")
    implementation("androidx.camera:camera-view:1.2.3")
    implementation("com.google.android.gms:play-services-maps:18.2.0")
    implementation("org.osmdroid:osmdroid-android:6.1.16")

    implementation ("com.squareup.okhttp3:okhttp:4.9.3")
    implementation ("org.json:json:20210307")
    implementation("androidx.camera:camera-core:1.3.0")
    implementation("androidx.camera:camera-camera2:1.3.0")
    implementation("androidx.camera:camera-lifecycle:1.3.0")
    implementation("androidx.camera:camera-view:1.3.0")

    // ML Kit (To find faces in the image)
    implementation ("com.google.mlkit:face-detection:16.1.6")

    // TensorFlow Lite (To recognize who the face is)
    implementation ("org.tensorflow:tensorflow-lite:2.14.0")
    implementation ("org.tensorflow:tensorflow-lite-support:0.4.4")
    implementation ("org.tensorflow:tensorflow-lite-gpu:2.14.0")

    // --- Networking & Images ---
    implementation("com.squareup.okhttp3:okhttp:4.9.3")
    implementation("com.github.bumptech.glide:glide:4.16.0")
    annotationProcessor("com.github.bumptech.glide:compiler:4.16.0")

    // --- Standard Android Libs ---
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation("androidx.security:security-crypto:1.0.0")

    // --- Fix Navigation & Core Crashes ---
    implementation("androidx.navigation:navigation-fragment-ktx:2.7.7")
    implementation("androidx.navigation:navigation-ui-ktx:2.7.7")
    implementation("androidx.core:core-ktx:1.13.1")
    implementation ("com.google.firebase:firebase-messaging:23.4.0")
    // --- ⭐ SUPABASE (Strictly v2.6.1 for Kotlin 1.9) ---
    // We use the BOM (Bill of Materials) to ensure all versions match automatically
    implementation(platform("io.github.jan-tennert.supabase:bom:3.0.1"))
    implementation("io.github.jan-tennert.supabase:postgrest-kt")
    implementation("io.github.jan-tennert.supabase:storage-kt")
    implementation("io.github.jan-tennert.supabase:auth-kt") // ✅ Uses 'auth-kt'
    implementation("io.github.jan-tennert.supabase:realtime-kt")
    implementation("com.google.firebase:firebase-messaging:23.4.0")
    // ... Ktor ...
    implementation("io.ktor:ktor-client-okhttp:3.0.0-rc-1")
    // --- Testing ---
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)
}