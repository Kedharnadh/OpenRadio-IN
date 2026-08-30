import java.io.FileInputStream
import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

// Signing credentials come from android/keystore.properties (gitignored).
val keystoreProperties = Properties()
val keystorePropertiesFile = rootProject.file("keystore.properties")
if (keystorePropertiesFile.exists()) {
    keystoreProperties.load(FileInputStream(keystorePropertiesFile))
}

android {
    namespace = "dev.openradio.android"
    compileSdk = 36

    defaultConfig {
        applicationId = "in.openradio.android"
        minSdk = 26
        targetSdk = 35
        versionCode = 7
        versionName = "1.3.3"

        // Station data is served straight from the OpenRadio-IN git repo (GitHub Pages),
        // so new stations added to the repo appear in the app automatically after refresh.
        buildConfigField(
            "String",
            "STATIONS_URL",
            "\"https://kedharnadh.github.io/OpenRadio-IN/data/stations.json\""
        )
        buildConfigField(
            "String",
            "STATIONS_URL_FALLBACK",
            "\"https://raw.githubusercontent.com/Kedharnadh/OpenRadio-IN/main/database/stations.json\""
        )
        buildConfigField(
            "String",
            "HLS_PROXY_URL",
            "\"https://openradio-hls-proxy.kedharnadh1.workers.dev\""
        )
    }

    buildFeatures {
        compose = true
        buildConfig = true
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    kotlinOptions {
        jvmTarget = "17"
    }

    signingConfigs {
        if (keystorePropertiesFile.exists()) {
            create("release") {
                storeFile = rootProject.file(keystoreProperties.getProperty("storeFile"))
                storePassword = keystoreProperties.getProperty("storePassword")
                keyAlias = keystoreProperties.getProperty("keyAlias")
                keyPassword = keystoreProperties.getProperty("keyPassword")
            }
        }
    }

    buildTypes {
        release {
            isMinifyEnabled = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            if (keystorePropertiesFile.exists()) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }

    packaging {
        resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
    }

    // Release-lint validates against the vital checks but its cache conflicts with
    // OneDrive's file locks on this machine, so we disable it for local builds.
    lint {
        checkReleaseBuilds = false
        abortOnError = false
    }
}

dependencies {
    // Compose (Material 3 / Material You)
    implementation(platform("androidx.compose:compose-bom:2024.12.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.material:material-icons-extended")
    implementation("androidx.compose.ui:ui-tooling-preview")
    debugImplementation("androidx.compose.ui:ui-tooling")

    // Lifecycle / Activity
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.7")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.7")

    // Coroutines
    implementation("org.jetbrains.kotlinx:kotlinx-coroutines-android:1.9.0")

    // Networking + images
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("io.coil-kt:coil-compose:2.7.0")

    // Media3: local + HLS playback, MediaSession (Android Auto / lock screen), Cast
    implementation("androidx.media3:media3-exoplayer:1.10.1")
    implementation("androidx.media3:media3-exoplayer-hls:1.10.1")
    implementation("androidx.media3:media3-session:1.10.1")
    implementation("androidx.media3:media3-cast:1.10.1")
    implementation("com.google.android.gms:play-services-cast-framework:21.5.0")
    implementation("androidx.mediarouter:mediarouter:1.8.1")
    implementation("com.google.guava:guava:33.3.1-android")
}
