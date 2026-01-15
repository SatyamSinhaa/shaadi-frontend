plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    id("org.jetbrains.kotlin.plugin.serialization")
    id("com.google.gms.google-services")
}

android {
    namespace = "com.example.myapplication"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.example.myapplication"
        minSdk = 26
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
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.iconsExtended)
    implementation(libs.retrofit)
    implementation(libs.retrofit.gson)
    implementation(libs.gson)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.coil.compose)
    implementation(libs.okhttp)
    
    // Cloudflare R2 - using signed URLs, no client library needed

    // Firebase
    implementation(platform("com.google.firebase:firebase-bom:33.7.0"))
    implementation("com.google.firebase:firebase-messaging-ktx")
    implementation("com.google.firebase:firebase-auth-ktx")
    implementation("com.google.android.gms:play-services-auth:21.2.0")

    // RxJava for STOMP WebSocket client
    implementation("io.reactivex.rxjava2:rxjava:2.2.21")
    implementation("io.reactivex.rxjava2:rxandroid:2.1.1")
    // STOMP over WebSocket for real-time messaging
    implementation("com.github.NaikSoftware:StompProtocolAndroid:1.6.6")
    
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
    implementation("com.github.yalantis:ucrop:2.2.8")
}

// Task to copy Debug APKs
tasks.register<Copy>("copyDebugApk") {
    description = "Copies generated Debug APKs to the root generated-apks/debug directory"
    from(layout.buildDirectory.dir("outputs/apk/debug"))
    into(rootProject.layout.projectDirectory.dir("generated-apks/debug"))
    include("*.apk")
    dependsOn("packageDebug")
}

// Task to copy Release APKs
tasks.register<Copy>("copyReleaseApk") {
    description = "Copies generated Release APKs to the root generated-apks/release directory"
    from(layout.buildDirectory.dir("outputs/apk/release"))
    into(rootProject.layout.projectDirectory.dir("generated-apks/release"))
    include("*.apk")
    dependsOn("packageRelease")
}

// Automatically run the copy tasks after assemble
afterEvaluate {
    tasks.named("assembleDebug") {
        finalizedBy("copyDebugApk")
    }
    tasks.named("assembleRelease") {
        finalizedBy("copyReleaseApk")
    }
}
