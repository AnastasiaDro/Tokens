pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
    plugins {
        id("com.android.settings") version "9.0.1" apply false
    }
}

plugins {
    id("com.android.settings")
}

android {
    compileSdk {
        version = release(36)
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "Tokens"
include(":app")
include(":feature:tokens_feature")
include(":feature:reinforcement_photo:api")
include(":feature:reinforcement_photo:impl")

// data layer
include(":core:ui")
include(":core:logger")
include(":data:reinforcement")
