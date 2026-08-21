import com.lagradost.cloudstream3.gradle.CloudstreamExtension

plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
}

android {
    namespace = "com.keyiflerolsun"
    compileSdk = 34

    defaultConfig {
        minSdk = 21
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_1_8
        targetCompatibility = JavaVersion.VERSION_1_8
    }

    kotlinOptions {
        jvmTarget = "1.8"
    }
}

cloudstream {
    extension("Diziyo") {
        classUrl = "com.keyiflerolsun.Diziyo"
        name = "Diziyo"
        description = "Diziyo Cloudstream Eklentisi"
        version = 1
        authors = listOf("keyiflerolsun")
    }
}
