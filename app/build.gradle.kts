plugins {
    alias(libs.plugins.androidApplication)
    alias(libs.plugins.navigationSafeArgs)
}

android {
    namespace = "com.cerebus.tokens"

    defaultConfig {
        applicationId = "com.cerebus.tokens_new"
        minSdk = 28
        targetSdk = 36
        versionCode = 8
        versionName = "2.1"

        testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        vectorDrawables {
            useSupportLibrary = true
        }
    }

    buildTypes {
        getByName("release") {
            isMinifyEnabled = true
            isShrinkResources = true
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
    implementation(libs.androidxCardview)

    implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.jar"))))
    implementation(libs.androidxAppcompat)
    implementation(libs.constraintlayout)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidxTestJunit)
    androidTestImplementation(libs.androidxTestEspresso)

    implementation(libs.androidxCoreKtx)
    implementation(libs.androidxLifecycleRuntimeKtx)
    implementation(libs.material)
    testImplementation(libs.junit)
    androidTestImplementation(libs.androidxTestJunit)
    androidTestImplementation(libs.androidxTestEspresso)

    implementation(libs.androidxLifecycleRuntimeKtx)
    implementation(libs.androidxLifecycleViewmodelKtx)

    implementation(libs.androidxActivityKtx)
    implementation(libs.androidxFragmentKtx)

    implementation(libs.viewbindingDelegate)

    // Lottie
    implementation(libs.lottie)

    // navigation
    implementation(libs.navigationFragmentKtx)
    implementation(libs.navigationUiKtx)
    // Dynamic Feature Module Support
    implementation(libs.navigationDynamicFeaturesFragment)

    // Testing Navigation
    androidTestImplementation(libs.navigationTesting)

    // Clean layers
    implementation(project(":feature:tokens_feature:api"))
    implementation(project(":feature:tokens_feature:impl"))
    implementation(project(":feature:reinforcement_photo:impl"))
    implementation(project(":feature:reinforcement_photo:api"))
    implementation(project(":core:logger"))
    implementation(project(":data:reinforcement"))

    // Koin
    implementation(libs.koinCore)
    implementation(libs.koinAndroid)
    implementation(libs.koinTest)
}
