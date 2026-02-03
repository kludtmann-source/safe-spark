plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    // ✅ KAPT für Room Database (stabiler als KSP)
    id("org.jetbrains.kotlin.kapt")
}

android {
    namespace = "com.example.safespark"
    compileSdk = 36

    lint {
        abortOnError = false
    }

    packaging {
        jniLibs {
            useLegacyPackaging = false
            // Explizite 16 KB Page Alignment
            pickFirsts += setOf("**/*.so")
        }
        resources {
            excludes += setOf(
                "META-INF/DEPENDENCIES",
                "META-INF/LICENSE",
                "META-INF/LICENSE.txt",
                "META-INF/license.txt",
                "META-INF/NOTICE",
                "META-INF/NOTICE.txt",
                "META-INF/notice.txt",
                "META-INF/ASL2.0",
                "META-INF/*.kotlin_module"
            )
        }
    }

    androidResources {
        ignoreAssetsPattern = "!.svn:!.git:!.ds_store:!*.scc:.*:!CVS:!thumbs.db:!picasa.ini:!*~"
    }

    defaultConfig {
        applicationId = "com.example.safespark"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // ✅ MULTIDEX AKTIVIERT (für große Apps mit vielen Klassen)
        multiDexEnabled = true

        // Explizite Unterstützung für 16 KB Page Size
        ndk {
            abiFilters.addAll(setOf("armeabi-v7a", "arm64-v8a", "x86", "x86_64"))
        }
    }

    // ✅ Source Sets für Test-Embeddings
    sourceSets {
        getByName("androidTest") {
            assets.srcDirs("src/androidTest/assets")
        }
        getByName("test") {
            resources.srcDirs("src/test/resources")
        }
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
}


dependencies {
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)

    // ✅ MULTIDEX Support (für große Apps)
    implementation("androidx.multidex:multidex:2.0.1")

    // Room Database ✅ AKTIVIERT (mit KAPT statt KSP)
    val room_version = "2.5.2"
    implementation("androidx.room:room-runtime:$room_version")
    implementation("androidx.room:room-ktx:$room_version")
    kapt("androidx.room:room-compiler:$room_version")

    // Lifecycle & ViewModel
    implementation("androidx.lifecycle:lifecycle-viewmodel-ktx:2.7.0")
    implementation("androidx.lifecycle:lifecycle-livedata-ktx:2.7.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3")
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")

    // Security - Encrypted SharedPreferences
    implementation("androidx.security:security-crypto:1.1.0-alpha06")

    // Testing
    testImplementation(libs.junit)
    testImplementation("org.mockito:mockito-core:5.7.0")
    testImplementation("org.mockito.kotlin:mockito-kotlin:5.1.0")
    testImplementation("org.mockito:mockito-inline:5.2.0")
    testImplementation("androidx.arch.core:core-testing:2.2.0")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
    testImplementation("com.google.truth:truth:1.1.5")

    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation("androidx.test:runner:1.5.2")
    androidTestImplementation("androidx.test:rules:1.5.0")
    androidTestImplementation("com.google.truth:truth:1.1.5")  // For semantic tests

    // TensorFlow Lite - using version 2.17.0 which has better 16 KB page size support
    implementation("org.tensorflow:tensorflow-lite:2.17.0")

    // ONNX Runtime for Semantic Detection (Sentence Encoder)
    implementation("com.microsoft.onnxruntime:onnxruntime-android:1.16.0")
}