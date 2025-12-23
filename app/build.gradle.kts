plugins {
    alias(libs.plugins.android.application)
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
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }
    buildFeatures {
        viewBinding = true
    }
}

dependencies {
    implementation ("com.github.PhilJay:MPAndroidChart:v3.1.0")
    // Google ML Kit for Text Recognition (Offline version)
    implementation ("com.google.android.gms:play-services-mlkit-text-recognition:19.0.0")

// CameraX (For easier camera handling)
    implementation ("androidx.camera:camera-core:1.2.3")
    implementation("com.google.android.gms:play-services-maps:18.2.0")
    implementation ("com.squareup.okhttp3:okhttp:4.9.3")
    implementation("org.osmdroid:osmdroid-android:6.1.16")
    implementation ("androidx.camera:camera-lifecycle:1.2.3")
    implementation ("androidx.camera:camera-view:1.2.3")
    implementation ("com.google.android.gms:play-services-mlkit-document-scanner:16.0.0-beta1")
    implementation(libs.appcompat)
    implementation(libs.material)
    implementation(libs.activity)
    implementation(libs.constraintlayout)
    implementation(libs.navigation.fragment)
    implementation(libs.navigation.ui)
    implementation("androidx.security:security-crypto:1.0.0")
    testImplementation(libs.junit)
    androidTestImplementation(libs.ext.junit)
    androidTestImplementation(libs.espresso.core)



}