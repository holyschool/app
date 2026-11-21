plugins {
    alias(libs.plugins.android.test)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.androidx.baselineprofile)
}

android {
    namespace = "com.wiffles.edupage.baselineprofile"
    compileSdk = 36

    defaultConfig {
        minSdk = 28
        targetSdk = 36
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    // Profile the benchmark build type: it has release R8/shrinking but the debug signature, so
    // generating a profile never replaces (and never wipes) the signed-in app on the device.
    targetProjectPath = ":app"
    targetVariant = "benchmark"
    experimentalProperties["android.experimental.self-instrumenting"] = true

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    kotlinOptions {
        jvmTarget = "11"
    }
}

baselineProfile {
    // The device is API 33+, so a profile can be collected without root.
    useConnectedDevices = true
}

dependencies {
    implementation(libs.androidx.junit)
    implementation(libs.androidx.test.uiautomator)
    implementation(libs.androidx.benchmark.macro.junit4)
}