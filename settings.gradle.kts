/*
 * SPDX-FileCopyrightText: The LineageOS Project
 * SPDX-License-Identifier: Apache-2.0
 */

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
        maven("https://raw.githubusercontent.com/lineage-next/gradle-generatebp/v1.32/.m2")
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        maven("https://raw.githubusercontent.com/lineage-next/camerax-aperture/19f0b87aca798f728efc054e68a36da3b6e11456/.m2")
        google()
        mavenCentral()
    }
}

rootProject.name = "Aperture"
include(":app")
include(":lens_launcher")
