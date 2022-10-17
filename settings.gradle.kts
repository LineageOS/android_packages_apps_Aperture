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
        maven("https://raw.githubusercontent.com/luk1337/camerax_selfie/83094804d1b7e9c7147bccae6a00289bfda97bca/.m2")
        google()
        mavenCentral()
    }
}
rootProject.name = "Aperture"
include(":app")
