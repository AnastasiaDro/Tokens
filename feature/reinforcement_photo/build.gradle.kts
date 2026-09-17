plugins {
    alias(libs.plugins.androidLibrary)
}

android {
    namespace = "com.cerebus.tokens.reinforcement_photo"

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
        viewBinding = true
    }
}

dependencies {
    implementation(project(":core:logger"))
    implementation(project(":core:ui"))
    implementation(project(":data:reinforcement"))

    implementation(libs.androidxCoreKtx)
    implementation(libs.androidxAppcompat)
    implementation(libs.material)
    implementation(libs.navigationFragmentKtx)
    implementation(libs.navigationUiKtx)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidxTestJunit)
    androidTestImplementation(libs.androidxTestEspresso)
    implementation(libs.viewbindingDelegate)

    implementation(libs.koinAndroid)
    implementation(libs.koinTest)
}
