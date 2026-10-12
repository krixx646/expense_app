plugins {
    alias(libs.plugins.android.application)
    // AGP 9 built-in Kotlin — do not apply org.jetbrains.kotlin.android
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
    // Keep auth optional: only apply if google-services.json exists (see bottom of file)
    alias(libs.plugins.google.services) apply false
}

android {
    namespace = "com.chelumore.xpenze"
    compileSdk = 36

    defaultConfig {
        applicationId = "com.chelumore.xpenze"
        minSdk = 26
        targetSdk = 36
        versionCode = 25
        versionName = "1.0.24"
        fun String.escapeForBuildConfig(): String = replace("\\", "\\\\").replace("\"", "\\\"")

        val geminiApiKey = (project.findProperty("GEMINI_API_KEY") as String?)
            ?: System.getenv("GEMINI_API_KEY")
            ?: ""
        val escapedGeminiApiKey = geminiApiKey.escapeForBuildConfig()
        buildConfigField("String", "GEMINI_API_KEY", "\"$escapedGeminiApiKey\"")
        buildConfigField("String", "GEMINI_MODEL", "\"gemini-2.5-flash\"")
        val firebaseFunctionsRegion = ((project.findProperty("FIREBASE_FUNCTIONS_REGION") as String?)
            ?: System.getenv("FIREBASE_FUNCTIONS_REGION")
            ?: "us-central1")
            .trim()
            .ifBlank { "us-central1" }
        buildConfigField(
            "String",
            "FIREBASE_FUNCTIONS_REGION",
            "\"${firebaseFunctionsRegion.escapeForBuildConfig()}\""
        )
        val supabaseProjectUrl = ((project.findProperty("SUPABASE_PROJECT_URL") as String?)
            ?: System.getenv("SUPABASE_PROJECT_URL")
            ?: "https://dglblftmgakhryfthxbh.supabase.co")
            .trim()
        buildConfigField(
            "String",
            "SUPABASE_PROJECT_URL",
            "\"${supabaseProjectUrl.escapeForBuildConfig()}\""
        )

        val admobAppId = ((project.findProperty("ADMOB_APP_ID") as String?)
            ?: System.getenv("ADMOB_APP_ID")
            ?: "ca-app-pub-3940256099942544~3347511713")
            .trim()
            .ifBlank { "ca-app-pub-3940256099942544~3347511713" }
        val admobBannerDashboard = ((project.findProperty("ADMOB_BANNER_DASHBOARD") as String?)
            ?: System.getenv("ADMOB_BANNER_DASHBOARD")
            ?: "").trim()
        val admobBannerTransactions = ((project.findProperty("ADMOB_BANNER_TRANSACTIONS") as String?)
            ?: System.getenv("ADMOB_BANNER_TRANSACTIONS")
            ?: "").trim()
        val admobBannerBudgets = ((project.findProperty("ADMOB_BANNER_BUDGETS") as String?)
            ?: System.getenv("ADMOB_BANNER_BUDGETS")
            ?: "").trim()
        val admobBannerReports = ((project.findProperty("ADMOB_BANNER_REPORTS") as String?)
            ?: System.getenv("ADMOB_BANNER_REPORTS")
            ?: "").trim()

        manifestPlaceholders["admobAppId"] = admobAppId
        buildConfigField("String", "ADMOB_APP_ID", "\"${admobAppId.escapeForBuildConfig()}\"")
        buildConfigField(
            "String",
            "ADMOB_BANNER_DASHBOARD",
            "\"${admobBannerDashboard.escapeForBuildConfig()}\""
        )
        buildConfigField(
            "String",
            "ADMOB_BANNER_TRANSACTIONS",
            "\"${admobBannerTransactions.escapeForBuildConfig()}\""
        )
        buildConfigField(
            "String",
            "ADMOB_BANNER_BUDGETS",
            "\"${admobBannerBudgets.escapeForBuildConfig()}\""
        )
        buildConfigField(
            "String",
            "ADMOB_BANNER_REPORTS",
            "\"${admobBannerReports.escapeForBuildConfig()}\""
        )

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

        // Room schema export for migrations
        ksp {
            arg("room.schemaLocation", "$projectDir/schemas")
            arg("room.incremental", "true")
            arg("room.generateKotlin", "true")
        }
    }

    // Optional release signing via environment variables or gradle.properties.
    // Keeps local/repo builds working while enabling CI/production signing when configured.
    val releaseKeystorePath = System.getenv("XPENZE_KEYSTORE_PATH")
        ?: (project.findProperty("XPENZE_KEYSTORE_PATH") as String?)
    val releaseKeystorePassword = System.getenv("XPENZE_KEYSTORE_PASSWORD")
        ?: (project.findProperty("XPENZE_KEYSTORE_PASSWORD") as String?)
    val releaseKeyAlias = System.getenv("XPENZE_KEY_ALIAS")
        ?: (project.findProperty("XPENZE_KEY_ALIAS") as String?)
    val releaseKeyPassword = System.getenv("XPENZE_KEY_PASSWORD")
        ?: (project.findProperty("XPENZE_KEY_PASSWORD") as String?)

    val hasReleaseSigning = !releaseKeystorePath.isNullOrBlank() &&
        !releaseKeystorePassword.isNullOrBlank() &&
        !releaseKeyAlias.isNullOrBlank() &&
        !releaseKeyPassword.isNullOrBlank() &&
        file(releaseKeystorePath).exists()

    if (hasReleaseSigning) {
        signingConfigs {
            create("release") {
                storeFile = file(releaseKeystorePath!!)
                storePassword = releaseKeystorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
            }
        }
    }

    buildTypes {
        debug {
            isMinifyEnabled = false
            isDebuggable = true
        }
        release {
            isMinifyEnabled = true
            isShrinkResources = true
            isDebuggable = false
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
            if (hasReleaseSigning) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    // With built-in Kotlin, jvmTarget defaults to compileOptions.targetCompatibility.

    buildFeatures {
        compose = true
        buildConfig = true
    }

    packaging {
        resources {
            excludes += "/META-INF/{AL2.0,LGPL2.1}"
            // Needed for androidTest APK packaging (some test deps bundle duplicate LICENSE.md files).
            excludes += "META-INF/LICENSE.md"
            excludes += "META-INF/LICENSE-notice.md"
        }
    }

    bundle {
        language {
            // The in-app language switcher can select any supported locale, not just the device one.
            // Play must therefore install every language with the base APK instead of splitting them out.
            enableSplit = false
        }
    }

    lint {
        // Lint is currently crashing when analyzing test/generated sources (KSP + Kotlin FIR/K2 issue).
        // Keep lint on main sources, but skip test + generated source analysis so `:app:check` is stable.
        checkTestSources = false
        checkGeneratedSources = false
    }
}

dependencies {
    // Compose BOM
    implementation(platform(libs.androidx.compose.bom))
    implementation(libs.bundles.compose)
    debugImplementation(libs.bundles.compose.debug)

    // Adaptive layouts
    implementation("androidx.compose.material3:material3-window-size-class")

    // Activity & Navigation
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigation.compose)

    // AndroidX Core
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.splashscreen)

    // Lifecycle
    implementation(libs.bundles.lifecycle)

    // Room Database
    implementation(libs.bundles.room)
    ksp(libs.androidx.room.compiler)
    testImplementation(libs.androidx.room.testing)

    // Hilt Dependency Injection
    implementation(libs.bundles.hilt)
    ksp(libs.hilt.android.compiler)
    ksp(libs.hilt.compiler)

    // Hilt WorkManager
    implementation(libs.hilt.work)

    // Coroutines
    implementation(libs.bundles.coroutines)
    testImplementation(libs.kotlinx.coroutines.test)

    // DataStore
    implementation(libs.androidx.datastore.preferences)

    // WorkManager
    implementation(libs.androidx.work.runtime.ktx)
    androidTestImplementation(libs.androidx.work.testing)

    // Firebase Auth (Email/Password)
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.auth.ktx)
    implementation(libs.firebase.firestore.ktx)
    implementation(libs.firebase.analytics.ktx)
    implementation(libs.firebase.functions.ktx)
    implementation(libs.firebase.appcheck.playintegrity)
    debugImplementation(libs.firebase.appcheck.debug)

    // Google Auth / Credentials
    implementation(libs.bundles.credentials)
    implementation(libs.play.services.ads)

    // Play Billing
    implementation(libs.billing.ktx)
    implementation(libs.play.review)
    implementation(libs.play.review.ktx)

    // Networking
    implementation(libs.bundles.networking)

    // Charts (Vico)
    implementation(libs.bundles.vico)

    // Image Loading
    implementation(libs.coil.compose)

    // OCR
    implementation(libs.mlkit.text.recognition)

    // Security
    implementation(libs.androidx.security.crypto)

    // Testing
    testImplementation(libs.junit)
    testImplementation(libs.mockk)
    testImplementation(libs.turbine)
    androidTestImplementation(libs.androidx.junit)
    androidTestImplementation(libs.androidx.espresso.core)
    androidTestImplementation(platform(libs.androidx.compose.bom))
    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.mockk.android)
}

// Apply Google Services plugin only when config is present (so builds still work without Firebase).
if (file("google-services.json").exists()) {
    apply(plugin = "com.google.gms.google-services")
}
