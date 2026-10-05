plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.ksp)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.terinit.rhythmicmeditation"
    // compileSdk 37 is required by the Compose 1.12 / Lifecycle 2.11 artifacts
    // in the pinned BOM. targetSdk stays 36 until the app opts into API 37
    // runtime behavior in a later pass.
    compileSdk = 37

    defaultConfig {
        applicationId = "com.terinit.rhythmicmeditation"
        minSdk = 26
        targetSdk = 36
        versionCode = 2
        versionName = "1.0.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }

    // Companion apps in the Rhythmic ecosystem can opt into the local shared
    // signer for debug QA and direct internal release APKs. Configure it with
    // -PrhythmicSharedDebugKeystore=<path>; without that property, keep using
    // the machine's normal Android debug key. The keystore stays outside Git.
    val sharedDebugKeystore = providers.gradleProperty("rhythmicSharedDebugKeystore").orNull
    if (sharedDebugKeystore != null) {
        signingConfigs.create("sharedQaDebug") {
            storeFile = file(sharedDebugKeystore)
            storePassword = providers.gradleProperty("rhythmicSharedDebugStorePassword")
                .orElse("android").get()
            keyAlias = providers.gradleProperty("rhythmicSharedDebugKeyAlias")
                .orElse("androiddebugkey").get()
            keyPassword = providers.gradleProperty("rhythmicSharedDebugKeyPassword")
                .orElse("android").get()
        }
    }

    buildTypes {
        debug {
            if (sharedDebugKeystore != null) {
                signingConfig = signingConfigs.getByName("sharedQaDebug")
            }
        }
        release {
            if (sharedDebugKeystore != null) {
                signingConfig = signingConfigs.getByName("sharedQaDebug")
            }
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
        compose = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }
}

dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.runtime.ktx)
    implementation(libs.androidx.lifecycle.runtime.compose)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(libs.androidx.lifecycle.process)
    implementation(libs.androidx.activity.compose)

    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.androidx.compose.ui)
    implementation(libs.androidx.compose.ui.graphics)
    implementation(libs.androidx.compose.ui.tooling.preview)
    implementation(libs.androidx.compose.material3)
    implementation(libs.androidx.compose.material.icons.extended)
    implementation(libs.androidx.navigation.compose)

    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    ksp(libs.androidx.room.compiler)
    implementation(libs.androidx.datastore.preferences)

    implementation(libs.kotlinx.coroutines.android)

    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)

    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.test.core.ktx)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.runner)
    androidTestImplementation(libs.androidx.test.espresso.core)
    androidTestImplementation(libs.kotlinx.coroutines.test)

    debugImplementation(libs.androidx.compose.ui.tooling)
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
