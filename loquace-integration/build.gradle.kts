plugins {
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.legacyKapt)
}

android {
    namespace = "org.linphone.loquace_integration"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        minSdk = 28

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
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

    buildFeatures {
        viewBinding = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
}

dependencies {
    // Retrofit
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.retrofit2:converter-gson:2.9.0")
    // Encrypted storage
    implementation("androidx.security:security-crypto:1.1.0-alpha06")
    // ViewModel + coroutines
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.7.0")
    implementation("androidx.activity:activity-ktx:1.8.2")
    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    // Firebase
    implementation("com.google.firebase:firebase-messaging:23.4.1")
    implementation(libs.androidx.appcompat)
    // Room
    implementation(libs.room.runtime)
    implementation(libs.room.ktx)
    kapt(libs.room.compiler)
    // DB Encryption
    implementation(libs.sqlcipher)
    implementation(libs.sqlite.ktx)
    //LinphoneSDK
    implementation(libs.linphone)
}