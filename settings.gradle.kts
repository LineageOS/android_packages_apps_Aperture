pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
    }
}
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        maven("https://raw.githubusercontent.com/luk1337/camerax_selfie/4ec143fff3b6918fe1d9cf3787fc8424cb1b32b9/.m2")
        google()
        mavenCentral()
    }
}
rootProject.name = "Aperture"
include(":app")
