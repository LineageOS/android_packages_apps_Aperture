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
        maven("https://raw.githubusercontent.com/luk1337/camerax_selfie/ec72084ac641e975a31bd220fe645fa99fb41a15/.m2")
        google()
        mavenCentral()
    }
}
rootProject.name = "Aperture"
include(":app")
