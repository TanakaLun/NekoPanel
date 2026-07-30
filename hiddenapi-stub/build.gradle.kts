plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "io.tl.nekopanel.hiddenapi.stub"
    compileSdk = 37

    defaultConfig {
        minSdk = 33
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_25
        targetCompatibility = JavaVersion.VERSION_25
    }
}
