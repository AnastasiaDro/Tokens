plugins {
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.kotlinCompose)
}

android {
    namespace = "com.cerebus.tokens.feature.tokens_feature"

    defaultConfig {
        minSdk = 28

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
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
    implementation(project(":feature:tokens_feature:api"))
    implementation(project(":feature:reinforcement_photo:api"))
    implementation(libs.datastore)
    implementation(libs.serializationJson)
    testImplementation(libs.coroutinesTest)

    implementation(platform(libs.composeBom))
    androidTestImplementation(platform(libs.composeBom))
    implementation(libs.composeUi)
    implementation(libs.composeFoundation)
    implementation(libs.composeMaterial3)
    implementation(libs.androidxLifecycleRuntimeCompose)
    implementation(libs.composeUiToolingPreview)
    debugImplementation(libs.composeUiTooling)
    debugImplementation(libs.composeUiTestManifest)
    androidTestImplementation(libs.composeUiTestJunit4)

    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar"))))
    implementation(libs.androidxAppcompat)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidxTestJunit)
    androidTestImplementation(libs.androidxTestEspresso)

    implementation(libs.androidxCoreKtx)
    implementation(libs.androidxLifecycleRuntimeKtx)

    implementation(libs.androidxLifecycleViewmodelKtx)

    implementation(libs.androidxActivityKtx)

    // color picker
    implementation(libs.colorPickerCompose)

    // Lottie
    implementation(libs.lottie)
    implementation(libs.lottieCompose)

    // navigation
    implementation(libs.navigationCompose)

    // domain
    implementation(project(":core:ui"))
    implementation(project(":core:logger"))
    implementation(project(":data:reinforcement"))

    // Koin
    implementation(libs.koinCore)
    implementation(libs.koinAndroid)
    implementation(libs.koinTest)
}
