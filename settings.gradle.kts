pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
    plugins {
        id("com.android.settings") version "9.4.1" apply false
    }
}

plugins {
    id("com.android.settings")
}

android {
    compileSdk {
        version = release(37)
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
include(":feature:tokens_feature:api")
include(":feature:tokens_feature:impl")
include(":feature:reinforcement_photo:api")
include(":feature:reinforcement_photo:impl")

// data layer
include(":core:ui")
include(":core:logger")
include(":data:reinforcement")
