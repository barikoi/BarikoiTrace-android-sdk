plugins {
    id("com.android.library")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp")
    id("maven-publish")
}

android {
    namespace = "com.barikoi.barikoitrace"
    compileSdk = 35

    defaultConfig {
        minSdk = 24
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

    compileOptions {
        isCoreLibraryDesugaringEnabled = true
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    testOptions {
        unitTests {
            isIncludeAndroidResources = true
        }
    }

    publishing {
        singleVariant("release") {
            withSourcesJar()
            withJavadocJar()
        }
    }
}

dependencies {
    // AndroidX Core
    implementation("androidx.core:core-ktx:1.15.0")
    implementation("androidx.appcompat:appcompat:1.7.0")

    // Kotlin Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")

    // Networking - Retrofit + OkHttp
    implementation("com.squareup.retrofit2:retrofit:2.11.0")
    implementation("com.squareup.retrofit2:converter-gson:2.11.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    // MQTT - Paho
    implementation("com.github.hannesa2:paho.mqtt.android:4.4")

    // Location
    implementation("com.google.android.gms:play-services-location:21.3.0")

    // DataStore
    implementation("androidx.datastore:datastore-preferences:1.1.4")

    // Keystore-backed storage for credentials and user PII — the counterpart
    // to the iOS SDK's Keychain use. See storage/SecureStore.kt.
    implementation("androidx.security:security-crypto:1.1.0-alpha06")

    // Room
    implementation("androidx.room:room-runtime:2.6.1")
    implementation("androidx.room:room-ktx:2.6.1")
    ksp("androidx.room:room-compiler:2.6.1")

    // WorkManager
    implementation("androidx.work:work-runtime-ktx:2.10.0")

    // Gson
    implementation("com.google.code.gson:gson:2.11.0")

    // Java 8+ API desugaring
    coreLibraryDesugaring("com.android.tools:desugar_jdk_libs:2.1.4")

    // Testing
    testImplementation("junit:junit:4.13.2")
    testImplementation("io.mockk:mockk:1.13.13")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.9.0")
    testImplementation("com.google.truth:truth:1.4.4")
    testImplementation("org.robolectric:robolectric:4.13")
    testImplementation("androidx.test:core:1.6.1")
    testImplementation("androidx.test.ext:junit:1.2.1")

    androidTestImplementation("androidx.test.ext:junit:1.2.1")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.6.1")
    androidTestImplementation("androidx.room:room-testing:2.6.1")
    androidTestImplementation("com.google.truth:truth:1.4.4")
    androidTestImplementation("androidx.test:core:1.6.1")
}

publishing {
    publications {
        register<MavenPublication>("release") {
            groupId = "com.github.barikoi"
            artifactId = "barikoitrace"
            // Bumped for the breaking BarikoiTrace.initialize() signature
            // change (added required mqttUsername/mqttPassword params,
            // removed the hardcoded broker credentials) — see
            // docs/SDK_DOCUMENTATION.md §6 finding #1's update.
            version = "2.0.0"

            afterEvaluate {
                from(components["release"])
            }
        }
    }
}
