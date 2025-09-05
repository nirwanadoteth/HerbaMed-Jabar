import java.util.Properties

val localPropertiesFile = rootProject.file("local.properties")
val properties = Properties()

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt.android)
    alias(libs.plugins.gms)
    alias(libs.plugins.kotlin.parcelize)
    alias(libs.plugins.ktfmt)
    alias(libs.plugins.room)
}

ktfmt { kotlinLangStyle() }

android {
    namespace = "edu.unikom.herbamedjabar"
    compileSdk = 36

    defaultConfig {
        applicationId = "edu.unikom.herbamedjabar"
        minSdk = 24
        targetSdk = 35
        versionCode = 1
        versionName = "1.0"
        vectorDrawables.useSupportLibrary = true

        if (localPropertiesFile.exists()) {
            localPropertiesFile.inputStream().use(properties::load)
        }

        val apiKey = properties.getProperty("apiKey", "")
        resValue("string", "api_key", apiKey)
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
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }

    buildFeatures {
        buildConfig = true
        viewBinding = true
    }

    packaging { resources { excludes += "/META-INF/{AL2.0,LGPL2.1}" } }

    room {
        schemaDirectory("$projectDir/schemas")
    }
}

dependencies {

    // Cloudinary
    implementation(libs.cloudinary.android)
    implementation(libs.androidx.core.splashscreen)

    // Firebase
    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.firestore)
    implementation(libs.firebase.storage)
    implementation(libs.firebase.auth)
    implementation(libs.credentials)
    implementation(libs.credentials.play.services.auth)
    implementation(libs.googleid)

    implementation(libs.lottie)
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.appcompat)
    implementation(libs.material)
    implementation(libs.androidx.activity)
    implementation(libs.androidx.constraintlayout)
    debugImplementation(libs.firebase.appcheck.debug)

    // Gemini API (Generative AI)
    implementation(libs.generativeai)

    // CameraX untuk fungsionalitas kamera yang lebih mudah
    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.view)

    // Coroutines untuk menangani proses background (seperti panggilan API)
    implementation(libs.kotlinx.coroutines.android)
    implementation(libs.kotlinx.coroutines.play.services)

    // Coil untuk memuat gambar dengan mudah
    implementation(libs.coil)

    // ViewModel and LiveData
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.livedata.ktx)
    implementation(libs.androidx.fragment.ktx)

    // Dagger - Hilt
    implementation(libs.hilt.android)
    ksp(libs.hilt.android.compiler)

    implementation(libs.androidx.room.runtime)
    ksp(libs.androidx.room.compiler)
    // Dukungan Room untuk Coroutines
    implementation(libs.androidx.room.ktx)

    // Markdown rendering
    implementation(libs.markdown)
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}
