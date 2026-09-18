plugins {
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.kotlinCompose)
}

android {
    namespace = "com.cerebus.tokens.core.ui"

    defaultConfig {
        minSdk = 28

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        consumerProguardFiles("consumer-rules.pro")
    }

    buildTypes {
        getByName("release") {
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
        compose = true
    }
}

dependencies {
    implementation(libs.activityCompose)
    implementation(libs.koinAndroid)
    implementation(platform(libs.composeBom))
    implementation(libs.composeUi)
    implementation(libs.composeFoundation)
    implementation(libs.composeMaterial3)
    implementation(libs.composeUiToolingPreview)
    debugImplementation(libs.composeUiTooling)
    debugImplementation(libs.composeUiTestManifest)
    androidTestImplementation(platform(libs.composeBom))
    androidTestImplementation(libs.composeUiTestJunit4)

    implementation(libs.androidxCoreKtx)
    implementation(libs.androidxAppcompat)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidxTestJunit)
    androidTestImplementation(libs.androidxTestEspresso)

    // navigation
    implementation(libs.navigationCompose)
}
