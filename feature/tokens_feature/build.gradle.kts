plugins {
    alias(libs.plugins.kotlinSerialization)
    alias(libs.plugins.androidLibrary)
    alias(libs.plugins.kotlinCompose)
    alias(libs.plugins.navigationSafeArgs)
    alias(libs.plugins.kotlinParcelize)
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
        viewBinding = true
        compose = true
    }
}

dependencies {
    implementation(project(":feature:reinforcement_photo:api"))
    implementation(libs.datastore)
    implementation(libs.serializationJson)
    testImplementation(libs.coroutinesTest)
    implementation(libs.kotlinParcelizeRuntime)

    implementation(libs.androidxCardview)

    implementation(platform(libs.composeBom))
    androidTestImplementation(platform(libs.composeBom))
    implementation(libs.composeUi)
    implementation(libs.composeFoundation)
    implementation(libs.composeUiToolingPreview)

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

    // color picker
    implementation(libs.colorPicker)

    // Lottie
    implementation(libs.lottie)

    // navigation
    implementation(libs.navigationFragmentKtx)
    implementation(libs.navigationUiKtx)
    // Dynamic Feature Module Support
    implementation(libs.navigationDynamicFeaturesFragment)

    // Testing Navigation
    androidTestImplementation(libs.navigationTesting)

    // domain
    implementation(project(":core:ui"))
    implementation(project(":core:logger"))
    implementation(project(":data:reinforcement"))

    // Koin
    implementation(libs.koinCore)
    implementation(libs.koinAndroid)
    implementation(libs.koinTest)
}

configurations.configureEach {
    if (name.startsWith("kotlinCompilerPluginClasspath")) {
        dependencies.add(project.dependencies.create(libs.kotlinParcelizeCompiler.get()))
    }
}
