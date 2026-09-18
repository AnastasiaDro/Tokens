plugins {
    alias(libs.plugins.androidLibrary)
}

android {
    namespace = "com.cerebus.tokens.reinforcement_photo.api"
    defaultConfig { minSdk = 28 }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    api(libs.navigationFragmentKtx)
}
