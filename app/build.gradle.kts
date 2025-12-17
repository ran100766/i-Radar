plugins {
//    kotlin("android")
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
//    id("androidx.compose.compiler") // <-- required for Kotlin 2.0+
    id("org.jetbrains.kotlin.plugin.compose")  // <-- Compose Compiler plugin
    id("com.google.gms.google-services")
}

android {
    namespace = "com.example.i_radar"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.example.i_radar"
        minSdk = 24
        targetSdk = 34
        versionCode = 1
        versionName = "1.0"
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    // For Android Studio < 2025.2, you may need this block instead of kotlin { jvmToolchain(...) }
    // kotlinOptions {
    //     jvmTarget = "17"
    // }

    buildFeatures {
        compose = true
    }

    composeOptions {
        kotlinCompilerExtensionVersion = "1.6.0" // match your Kotlin version
    }

}

// KOTLIN JVM toolchain block must be OUTSIDE android { } in Kotlin DSL
kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.appcompat:appcompat:1.7.0")
    implementation("com.google.android.material:material:1.11.0")
    implementation("com.google.android.gms:play-services-location:21.0.1")

    val composeVersion = "1.6.0" // or latest compatible with your Kotlin

    implementation("androidx.activity:activity-compose:1.8.0")
    implementation("androidx.compose.ui:ui:$composeVersion")
    implementation("androidx.compose.ui:ui-tooling-preview:$composeVersion")
    implementation("androidx.compose.material3:material3:1.2.0") // if using Material3
    debugImplementation("androidx.compose.ui:ui-tooling:$composeVersion")
    implementation("androidx.activity:activity-compose:1.8.0")
    implementation("androidx.compose.ui:ui:$composeVersion")
    implementation("androidx.compose.ui:ui-tooling-preview:$composeVersion")
    implementation("androidx.compose.material3:material3:1.2.0")
    debugImplementation("androidx.compose.ui:ui-tooling:$composeVersion")
    testImplementation("junit:junit:4.13.2")
    androidTestImplementation("androidx.test.ext:junit:1.1.5") // JUnit 4 support for Android tests
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1") // UI testing

    implementation("com.squareup.okhttp3:okhttp:4.11.0")
    implementation("org.json:json:20230618")

    implementation(platform("com.google.firebase:firebase-bom:33.3.0"))
    implementation("com.google.firebase:firebase-firestore-ktx")
    implementation("com.google.firebase:firebase-auth-ktx")
    implementation("com.google.firebase:firebase-analytics-ktx")

}
