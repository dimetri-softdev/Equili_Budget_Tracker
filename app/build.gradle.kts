import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
    id("com.google.gms.google-services")
}

android {
    namespace = "com.example.equili"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.equili"
        minSdk = 24
        targetSdk = 36
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
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    buildFeatures {
        viewBinding = true
    }
}

kotlin {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_11)
    }
}

dependencies {

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.mpandroidchart)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)

    // add bellow dependencies
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.9.4")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.9.4")
    implementation ("androidx.room:room-runtime:2.8.3")
    ksp ("androidx.room:room-compiler:2.8.3")
    implementation ("androidx.room:room-ktx:2.8.3")
    implementation ("androidx.recyclerview:recyclerview:1.4.0")
    implementation ("com.google.android.material:material:1.13.0")

    // Firebase BoM
    implementation(platform("com.google.firebase:firebase-bom:33.1.0"))

// Firebase SDKs
    implementation("com.google.firebase:firebase-auth-ktx")
    implementation("com.google.firebase:firebase-firestore-ktx")
    implementation("com.google.firebase:firebase-storage-ktx") // For receipt images

// To use .await() with Firebase (makes it work like Room's suspend functions)
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-play-services:1.7.3")
}
