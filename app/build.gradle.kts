plugins {
    alias(libs.plugins.android.application)
}

android {
    namespace = "desu.tei.classicnetworkqs"
    compileSdk {
        version = release(37) {
            minorApiLevel = 0
        }
    }

    defaultConfig {
        applicationId = "desu.tei.classicnetworkqs"
        minSdk = 34
        targetSdk = 37
        val ciVersion = providers.gradleProperty("ciVersion").orNull
        versionCode = ciVersion?.toInt()?.also { require(it in 1..2100000000) } ?: 1
        versionName = ciVersion ?: "1.0"

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
}

dependencies {
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.core.ktx)
    implementation(libs.material)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(libs.androidx.junit)
}