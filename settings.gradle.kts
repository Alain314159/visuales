pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        // JitPack for occasional libraries (e.g., PRDownloader if we use it later)
        maven { url = uri("https://jitpack.io") }
    }
}

rootProject.name = "Visuales"

include(":app")
include(":core:common")
include(":core:designsystem")
include(":core:network")
include(":core:data")
include(":sync:listado")
include(":feature:catalog")
include(":feature:downloads")
include(":feature:library")
include(":feature:player")
include(":feature:settings")
