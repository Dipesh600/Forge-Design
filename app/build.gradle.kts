plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.kotlin.ksp)
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.google.services)
    alias(libs.plugins.kotlin.parcelize)
}

android {
    namespace   = "com.forge.vdesign"
    compileSdk {
        version = release(36)
    }

    defaultConfig {
        applicationId   = "com.forge.vdesign"
        minSdk          = 24
        targetSdk       = 36
        versionCode     = 2
        versionName     = "1.0.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Stitch server on Render — update STITCH_SERVER_URL after Render deployment
        buildConfigField("String", "STITCH_SERVER_URL",
            "\"${project.findProperty("STITCH_SERVER_URL") ?: "https://forge-stitch-server.onrender.com"}\"")
        buildConfigField("String", "STITCH_API_KEY",
            "\"${project.findProperty("STITCH_API_KEY") ?: "forge-dev-key"}\"")
    }

    buildTypes {
        debug {
            isMinifyEnabled   = false
            isDebuggable      = true
            applicationIdSuffix = ".debug"
            versionNameSuffix   = "-debug"
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
    }

    buildFeatures {
        viewBinding  = true
        buildConfig  = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    kotlinOptions {
        jvmTarget = "11"
    }

    // Room schema export for migration tracking
    ksp {
        arg("room.schemaLocation", "$projectDir/schemas")
        arg("room.incremental", "true")
    }
}

dependencies {
    // ── AndroidX Core ─────────────────────────────────────────────────────────
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.androidx.activity.ktx)
    implementation(libs.androidx.fragment.ktx)
    implementation(libs.androidx.constraintlayout)
    implementation(libs.androidx.recyclerview)
    implementation(libs.androidx.splashscreen)

    // ── Material Design 3 ─────────────────────────────────────────────────────
    implementation(libs.material)

    // ── Lifecycle ─────────────────────────────────────────────────────────────
    implementation(libs.androidx.lifecycle.viewmodel)
    implementation(libs.androidx.lifecycle.runtime)

    // ── Navigation Component ──────────────────────────────────────────────────
    implementation(libs.androidx.navigation.fragment)
    implementation(libs.androidx.navigation.ui)

    // ── Room Database ─────────────────────────────────────────────────────────
    implementation(libs.androidx.room.runtime)
    implementation(libs.androidx.room.ktx)
    implementation(libs.firebase.auth)
    implementation(libs.androidx.credentials)
    implementation(libs.androidx.credentials.play.services.auth)
    implementation(libs.googleid)
    ksp(libs.androidx.room.compiler)

    // ── DataStore ─────────────────────────────────────────────────────────────
    implementation(libs.androidx.datastore.prefs)

    // ── Coroutines ────────────────────────────────────────────────────────────
    implementation(libs.kotlinx.coroutines.android)

    // ── Networking (Retrofit + OkHttp) ────────────────────────────────────────
    implementation(libs.retrofit)
    implementation(libs.retrofit.converter.gson)
    implementation(platform(libs.okhttp.bom))
    implementation(libs.okhttp)
    implementation(libs.okhttp.logging)
    implementation(libs.gson)

    // ── Hilt Dependency Injection ─────────────────────────────────────────────
    implementation(libs.hilt.android)
    ksp(libs.hilt.compiler)

    // ── Firebase (BOM manages versions) ───────────────────────────────────────
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth.ktx)
    implementation(libs.firebase.firestore.ktx)
    implementation(libs.firebase.functions.ktx)
    implementation(libs.firebase.analytics.ktx)
    implementation(libs.firebase.storage.ktx)

    // ── Image Loading ─────────────────────────────────────────────────────────
    implementation("com.github.bumptech.glide:glide:4.16.0")
    ksp("com.github.bumptech.glide:ksp:4.16.0")

    // ── Markdown Rendering ────────────────────────────────────────────────────
    // Markwon renders markdown directly into Android TextViews
    implementation("io.noties.markwon:core:4.6.2")
    implementation("io.noties.markwon:image-glide:4.6.2")   // inline images via Glide
    implementation("io.noties.markwon:ext-strikethrough:4.6.2")
    implementation("io.noties.markwon:ext-tables:4.6.2")

    // ── Testing ───────────────────────────────────────────────────────────────
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
}