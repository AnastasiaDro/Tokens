plugins {
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.kotlinCompose)
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
        compose = true
    }
}

dependencies {
    implementation(libs.activityCompose)
    implementation(libs.serializationJson)
    implementation(platform(libs.composeBom))
    implementation(libs.composeUi)
    implementation(libs.composeFoundation)
    implementation(libs.composeMaterial3)
    implementation(libs.androidxLifecycleRuntimeCompose)
    debugImplementation(libs.composeUiTooling)
    debugImplementation(libs.composeUiTestManifest)
    androidTestImplementation(platform(libs.composeBom))
    androidTestImplementation(libs.composeUiTestJunit4)
    implementation(project(":feature:reinforcement_photo:api"))
    testImplementation(libs.coroutinesTest)
    implementation(project(":core:logger"))
    implementation(project(":core:ui"))
    implementation(project(":data:reinforcement"))

    implementation(libs.androidxCoreKtx)
    implementation(libs.androidxAppcompat)
    implementation(libs.navigationCompose)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidxTestJunit)
    androidTestImplementation(libs.androidxTestEspresso)

    implementation(libs.koinAndroid)
    implementation(libs.koinTest)
}
