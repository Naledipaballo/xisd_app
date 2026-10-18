plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("com.google.devtools.ksp")
    alias(libs.plugins.google.gms.google.services)
}

android {
    namespace = "com.example.pillpoint"
    compileSdk = 36

    buildFeatures {
        viewBinding = true
    }

    defaultConfig {
        applicationId = "com.example.pillpoint"
        minSdk = 24
        targetSdk = 36
        versionCode = 1
        versionName = "1.0"

        testInstrumentationRunner =
            "androidx.test.runner.AndroidJUnitRunner"
    }

    buildTypes {
        release {
            isMinifyEnabled = false

            proguardFiles(
                getDefaultProguardFile(
                    "proguard-android-optimize.txt"
                ),
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
    // Firebase BoM manages Firebase library versions.
    implementation(
        platform("com.google.firebase:firebase-bom:33.9.0")
    )

    // Firebase Authentication: email and password login.
    implementation("com.google.firebase:firebase-auth")

    // Cloud Firestore database.
    implementation("com.google.firebase:firebase-firestore")

    // Room Database.
    val roomVersion = "2.8.4"

    implementation("androidx.room:room-runtime:$roomVersion")
    implementation("androidx.room:room-ktx:$roomVersion")
    ksp("androidx.room:room-compiler:$roomVersion")

    // Lifecycle and coroutine scopes.
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")

    // AndroidX and Material Design.
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)

    // Navigation drawer.
    implementation("androidx.drawerlayout:drawerlayout:1.2.0")

    // Google Sign-In support already present in your project.
    implementation(libs.play.services.auth)

    // OpenStreetMap Store Locator.
    implementation("org.osmdroid:osmdroid-android:6.1.20")

    // Existing password-hashing library.
    implementation(libs.jbcrypt)

    // Unit and instrumentation testing.
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    testImplementation("junit:junit:4.13.2")

}