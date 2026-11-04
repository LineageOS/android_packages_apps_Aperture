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
        maven("https://raw.githubusercontent.com/lineage-next/gradle-generatebp/v1.33/.m2")
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        maven("https://raw.githubusercontent.com/lineage-next/camerax-aperture/55b687c1232294e908ef6f46e004a58e57a0e63d/.m2")
        google()
        mavenCentral()
    }
}

rootProject.name = "Aperture"
include(":app")
include(":lens_launcher")
