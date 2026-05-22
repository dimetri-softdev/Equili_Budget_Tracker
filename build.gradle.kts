// Top-level build file where you can add configuration options common to all sub-projects/modules.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.ksp) apply false
}

buildscript {
    dependencies {
        // Add the Google services classpath
        classpath("com.google.gms:google-services:4.4.2")
    }
}
