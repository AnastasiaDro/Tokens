plugins {
    alias(libs.plugins.kotlinCompose)
    alias(libs.plugins.androidApplication)
}

android {
    buildFeatures { compose = true }
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
}

dependencies {
    implementation(libs.activityCompose)
    implementation(platform(libs.composeBom))
    implementation(libs.composeUi)
    implementation(project(":core:ui"))
    androidTestImplementation(platform(libs.composeBom))
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

    // navigation
    implementation(libs.navigationCompose)

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
