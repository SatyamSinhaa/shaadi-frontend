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
        maven { url = uri("https://phonepe.mycloudrepo.io/public/repositories/phonepe-intent-sdk-android") }
        maven { url = uri("https://jitpack.io") }
        // Adding the official PhonePe maven repository as a fallback
        maven { url = uri("https://oss.sonatype.org/content/repositories/releases/") }
    }
}

rootProject.name = "My Application"
include(":app")
