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
        maven("https://raw.githubusercontent.com/luk1337/camerax_selfie/84db72d746f38fbf4def600aac6d730c4cb3dae6/.m2")
        google()
        mavenCentral()
    }
}
rootProject.name = "Aperture"
include(":app")
