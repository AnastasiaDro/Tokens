plugins {
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.androidLibrary)
}

val minimumAndroidSdk = 28

android {
    namespace = "com.cerebus.tokens.reinforcement_photo.api"
    defaultConfig { minSdk = minimumAndroidSdk }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    api(libs.navigationCompose)
    implementation(libs.serializationJson)
}
