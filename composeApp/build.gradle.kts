import org.jetbrains.compose.ExperimentalComposeLibrary
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.targets.native.tasks.KotlinNativeSimulatorTest

plugins {
    alias(libs.plugins.kotlinMultiplatform)
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.composeMultiplatform)
    alias(libs.plugins.composeCompiler)
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.ktlint)
}

kotlin {
    androidTarget {
        compilerOptions { jvmTarget.set(JvmTarget.JVM_11) }
    }

    listOf(iosX64(), iosArm64(), iosSimulatorArm64()).forEach { target ->
        target.binaries.framework {
            baseName = "ComposeApp"
            isStatic = true
        }
    }

    // JVM target exists only so commonTest (and Compose UI tests in jvmTest)
    // run fast on a desktop JVM. It is not shipped.
    jvm()

    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
        optIn.add("kotlin.time.ExperimentalTime")
    }

    sourceSets {
        commonMain.dependencies {
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.material3)
            implementation(compose.materialIconsExtended)
            implementation(compose.ui)
            implementation(compose.components.resources)
            implementation(libs.androidx.lifecycle.viewmodel.compose)
            implementation(libs.androidx.lifecycle.runtime.compose)
            implementation(libs.datastore.preferences.core)
            implementation(libs.okio)
            implementation(libs.ktor.network)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kotlinx.serialization.json)
            implementation(libs.kotlinx.datetime)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
            implementation(libs.kotlinx.coroutines.test)
        }
        androidMain.dependencies {
            implementation(libs.androidx.activity.compose)
            implementation(libs.androidx.core.ktx)
            implementation(libs.androidx.exifinterface)
            implementation(libs.androidx.window)
            implementation(libs.kotlinx.coroutines.android)
        }
        jvmMain.dependencies {
            implementation(compose.desktop.currentOs)
            implementation(libs.kotlinx.coroutines.swing)
        }
        androidInstrumentedTest.dependencies {
            implementation(libs.androidx.test.runner)
            implementation(libs.androidx.test.ext.junit)
            implementation(libs.androidx.compose.ui.test.junit4)
            // Compose ui-test pulls Espresso 3.5, which calls InputManager.getInstance() and crashes on API 37.
            implementation(libs.androidx.test.espresso.core)
        }
        jvmTest.dependencies {
            @OptIn(ExperimentalComposeLibrary::class)
            implementation(compose.uiTest)
        }
    }
}

// commonTest also runs on an iOS simulator (iosSimulatorArm64Test). CI passes
// -PiosSimulator=<UDID> to pick the newest simulator; otherwise Kotlin's default device is used.
tasks.withType<KotlinNativeSimulatorTest>().configureEach {
    providers.gradleProperty("iosSimulator").orNull?.let { device.set(it) }
}

compose.resources {
    publicResClass = false
    packageOfResClass = "com.lexterlabra.puzzle.resources"
    generateResClass = always
}

android {
    namespace = "com.lexterlabra.puzzle"
    compileSdk = libs.versions.android.compileSdk.get().toInt()

    defaultConfig {
        applicationId = "com.lexterlabra.puzzle"
        minSdk = libs.versions.android.minSdk.get().toInt()
        targetSdk = libs.versions.android.targetSdk.get().toInt()
        // CI passes -PversionCode=<run number> so every distributed build installs over the last.
        versionCode = providers.gradleProperty("versionCode").map(String::toInt).getOrElse(1)
        versionName = "1.0"
        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    }
    // Release signing comes from the environment (CI secrets); without it, release builds are unsigned.
    val releaseKeystore = providers.environmentVariable("ANDROID_KEYSTORE_PATH")
    signingConfigs {
        if (releaseKeystore.isPresent) {
            create("release") {
                storeFile = file(releaseKeystore.get())
                storePassword = providers.environmentVariable("ANDROID_KEYSTORE_PASSWORD").get()
                keyAlias = providers.environmentVariable("ANDROID_KEY_ALIAS").get()
                keyPassword = providers.environmentVariable("ANDROID_KEY_PASSWORD").get()
            }
        }
    }
    packaging {
        resources { excludes += "/META-INF/{AL2.0,LGPL2.1}" }
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            signingConfig = signingConfigs.findByName("release")
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }
    lint {
        // Security checks fail the build instead of warning (CI runs lintDebug).
        error += setOf(
            "AllowAllHostnameVerifier", "BadHostnameVerifier", "TrustAllX509TrustManager", "SSLCertificateSocketFactoryGetInsecure",
            "SSLCertificateSocketFactoryCreateSocket", "InsecureBaseConfiguration", "AcceptsUserCertificates", "HardcodedDebugMode",
            "SetJavaScriptEnabled", "AddJavascriptInterface", "ExportedContentProvider", "ExportedReceiver", "ExportedService",
            "ExportedPreferenceActivity", "GrantAllUris", "UnsafeIntentLaunch", "UnsafeProtectedBroadcastReceiver",
            "WorldReadableFiles", "WorldWriteableFiles", "SecureRandom", "GetInstance", "PackageManagerGetSignatures", "LogConditional",
        )
    }
}

dependencies {
    debugImplementation(libs.androidx.compose.ui.test.manifest)
}
