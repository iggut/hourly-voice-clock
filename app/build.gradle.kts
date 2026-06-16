plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

// Allow versionName and versionCode to be overridden from
// gradle.properties (so CI can set them from the version tag) without
// the default values drifting from the source of truth in this file.
val versionMajor: Int = (project.findProperty("version.major") as String?)?.toIntOrNull() ?: 0
val versionMinor: Int = (project.findProperty("version.minor") as String?)?.toIntOrNull() ?: 4
val versionPatch: Int = (project.findProperty("version.patch") as String?)?.toIntOrNull() ?: 9
val versionPre: String = (project.findProperty("version.pre") as String?) ?: "alpha"
// versionCode is a monotonically increasing integer; compute it from
// the components above so we never forget to bump it.
val computedVersionCode: Int = versionMajor * 100_000 + versionMinor * 1_000 + versionPatch
val versionNameOverride: String? = project.findProperty("versionName") as String?

android {
    namespace = "com.hourlyvoiceclock"
    compileSdk = 34

    defaultConfig {
        applicationId = "com.hourlyvoiceclock"
        minSdk = 26
        targetSdk = 34
        versionCode = (project.findProperty("versionCode") as String?)?.toIntOrNull() ?: computedVersionCode
        versionName = versionNameOverride ?: "${versionMajor}.${versionMinor}.${versionPatch}-${versionPre}"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
        // Languages we explicitly support. Play Store uses this list to
        // filter the app in non-supported locales and to make the
        // "supported languages" section of the listing accurate. We
        // support English and French (TTS engine wraps both, the UI is
        // English-only today; declared now so the listing is honest).
        resourceConfigurations += listOf("en", "fr")
    }

    signingConfigs {
        create("release") {
            val envKeystorePath = System.getenv("KEYSTORE_PATH")
            val envKeystorePassword = System.getenv("KEYSTORE_PASSWORD")
            val envKeyAlias = System.getenv("KEY_ALIAS")
            val envKeyPassword = System.getenv("KEY_PASSWORD")

            if (envKeystorePath != null && file(envKeystorePath).exists() && file(envKeystorePath).length() > 0) {
                storeFile = file(envKeystorePath)
                storePassword = envKeystorePassword ?: ""
                keyAlias = envKeyAlias ?: ""
                keyPassword = envKeyPassword ?: ""
            } else {
                // Fallback to the debug keystore so local builds work
                // without release credentials. The CI workflow sets
                // KEYSTORE_PATH etc. from secrets; local dev without
                // a real key still gets a signed (debug-key) APK.
                storeFile = file("${System.getProperty("user.home")}/.android/debug.keystore")
                storePassword = "android"
                keyAlias = "androiddebugkey"
                keyPassword = "android"
            }
        }
    }

    buildTypes {
        release {
            // R8 in full mode: shrink, optimise, and obfuscate. The
            // resulting APK is ~5x smaller and the obfuscation makes
            // reverse engineering the announcement logic marginally
            // harder. Required for Play Store as of Aug 2024 for new
            // apps. shrinkResources drops unreferenced drawables/strings.
            isMinifyEnabled = true
            isShrinkResources = true
            signingConfig = signingConfigs.getByName("release")
            proguardFiles(
                getDefaultProguardFile("proguard-android-optimize.txt"),
                "proguard-rules.pro"
            )
        }
        debug {
            // Keep debug builds readable for stack traces.
            isMinifyEnabled = false
        }
    }

    // Android App Bundle output config. Play Store requires .aab for
    // new submissions; the Gradle task is `bundleRelease` (vs the
    // legacy `assembleRelease` which produces .apk).
    bundle {
        language {
            enableSplit = true
        }
        density {
            enableSplit = true
        }
        abi {
            enableSplit = true
        }
    }

    lint {
        disable += "StateFlowValueCalledInComposition"
        // Enable lint on release builds; the baseline file documents
        // the pre-existing issues we know about and intentionally
        // leave in place. New lint errors will fail the build.
        checkReleaseBuilds = true
        abortOnError = true
        baseline = file("lint-baseline.xml")
        // Be strict about correctness issues but quiet about
        // library-version churn. The user can refresh dependencies on
        // their own schedule.
        disable += "GradleDependency"
        // OldTargetApi is noisy while we sit on targetSdk 34; we will
        // bump to 35 in a follow-up that also addresses the
        // behavioral changes (16KB pages, edge-to-edge).
        disable += "OldTargetApi"
        disable += "MonochromeLauncherIcon"
        disable += "TypographyEllipsis"
        disable += "UnusedResources"
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_21
        targetCompatibility = JavaVersion.VERSION_21
    }
    kotlin {
        jvmToolchain(21)
        compilerOptions {
            jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_21)
        }
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
    // Core
    implementation("androidx.core:core-ktx:1.12.0")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.7.0")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.7.0")
    implementation("androidx.activity:activity-compose:1.8.2")

    // Compose BOM
    val composeBom = platform("androidx.compose:compose-bom:2024.02.00")
    implementation(composeBom)
    androidTestImplementation(composeBom)

    // Compose
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.ui:ui-graphics")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")

    // Network
    implementation("com.squareup.okhttp3:okhttp:4.12.0")

    // Local TTS (Sherpa-ONNX) - AAR from GitHub releases
    implementation(files("libs/sherpa-onnx-1.13.2.aar"))

    // Navigation
    implementation("androidx.navigation:navigation-compose:2.7.7")

    // DataStore
    implementation("androidx.datastore:datastore-preferences:1.0.0")
    implementation("androidx.datastore:datastore-core:1.0.0")

    // Testing
    testImplementation("junit:junit:4.13.2")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
    testImplementation("org.robolectric:robolectric:4.11.1")
    testImplementation("org.mockito:mockito-core:5.10.0")
    androidTestImplementation("androidx.test.ext:junit:1.1.5")
    androidTestImplementation("androidx.test.espresso:espresso-core:3.5.1")
    androidTestImplementation("androidx.compose.ui:ui-test-junit4")
    debugImplementation("androidx.compose.ui:ui-tooling")
    debugImplementation("androidx.compose.ui:ui-test-manifest")
}
