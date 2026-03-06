plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.hilt)
    alias(libs.plugins.kotlin.kapt)
}

// Pin the Java toolchain to OpenJDK 17 (/usr/lib/jvm/java-17-openjdk).
// - OpenJDK 17 has `jlink`, which AGP's JdkImageTransform requires.
// - Class files it produces (version 61) are readable by the IDE's JBR (Java 21).
// - The jvm.toolchains.install.locations property in gradle.properties ensures
//   Gradle resolves this even when the daemon is launched by the IDE's JBR.
java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
    }
}

android {
    namespace = "com.enderplusbayzuiship.edupage2"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.enderplusbayzuiship.edupage2"
        minSdk = 26
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
        isCoreLibraryDesugaringEnabled = true
    }
    kotlinOptions {
        jvmTarget = "11"
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    // EduPage library
    implementation(project(":edupage-lib"))

    // AndroidX core
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.activity.compose)

    // Compose BOM
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.ui)
    implementation(libs.androidx.ui.graphics)
    implementation(libs.androidx.ui.tooling.preview)
    implementation(libs.androidx.material3)

    // Navigation
    implementation(libs.androidx.navigation.compose)

    // ViewModel + Compose
    implementation(libs.androidx.lifecycle.viewmodel.compose)

    // Hilt
    implementation(libs.hilt.android)
    kapt(libs.hilt.compiler)
    implementation(libs.hilt.navigation.compose)
    implementation(libs.hilt.work)
    kapt(libs.hilt.work.compiler)

    // WorkManager
    implementation(libs.androidx.work.runtime.ktx)

    // Coroutines
    implementation(libs.kotlinx.coroutines.android)

    // Security — EncryptedSharedPreferences
    implementation(libs.androidx.security.crypto)

    // Networking — needed because edupage-lib exposes OkHttp/Gson types in its public API
    implementation(libs.okhttp)
    implementation(libs.gson)

    // Graphics shapes — morphing loading indicator
    implementation(libs.androidx.graphics.shapes)

    // Splash screen API (suppress OS splash icon flash)
    implementation(libs.androidx.core.splashscreen)

    // Desugaring (for java.time on API < 26, belt-and-suspenders)
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.1.5")

    // Test
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.ui.test.junit4)
    debugImplementation(libs.androidx.ui.tooling)
    debugImplementation(libs.androidx.ui.test.manifest)
}
